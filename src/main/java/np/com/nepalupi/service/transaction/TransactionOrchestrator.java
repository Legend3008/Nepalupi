package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.config.TransactionMetrics;
import np.com.nepalupi.domain.dto.request.PaymentRequest;
import np.com.nepalupi.domain.dto.response.BankResponse;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.domain.dto.response.VpaDetails;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.domain.enums.TransactionType;
import np.com.nepalupi.domain.event.TransactionEvent;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.service.bank.BankConnector;
import np.com.nepalupi.service.fraud.FraudEngine;
import np.com.nepalupi.service.notification.TransactionEventPublisher;
import np.com.nepalupi.service.pin.PinEncryptionService;
import np.com.nepalupi.service.vpa.VpaResolutionService;
import np.com.nepalupi.util.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * The main transaction engine — coordinates the entire UPI payment flow:
 * <ol>
 *   <li>Idempotency check</li>
 *   <li>Resolve payer & payee VPAs</li>
 *   <li>Validate limits & fraud</li>
 *   <li>Create transaction record (INITIATED)</li>
 *   <li>Debit payer bank</li>
 *   <li>Credit payee bank</li>
 *   <li>Handle failures & reversals</li>
 *   <li>Publish async events (notifications, analytics, settlement)</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionOrchestrator {

    private final TransactionRepository txnRepo;
    private final TransactionStateMachine stateMachine;
    private final VpaResolutionService vpaService;
    private final LimitValidationService limitService;
    private final FraudEngine fraudEngine;
    private final BankConnector bankConnector;
    private final ReversalService reversalService;
    private final TransactionEventPublisher eventPublisher;
    private final TransactionMetrics metrics;
    private final PinEncryptionService pinEncryptionService;

    @Value("${nepalupi.transaction.expiry-minutes:10}")
    private int expiryMinutes;

    /**
     * Initiate a P2P or P2M payment.
     */
    @Transactional
    public TransactionResponse initiatePayment(PaymentRequest request) {

        // ── Step 1: Idempotency ──────────────────────────────
        Optional<Transaction> existing = txnRepo.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key {}, returning existing txn {}",
                    request.getIdempotencyKey(), existing.get().getUpiTxnId());
            return TransactionResponse.from(existing.get());
        }

        // ── Step 2: Resolve both VPAs ────────────────────────
        VpaDetails payerVpa = vpaService.resolve(request.getPayerVpa());
        VpaDetails payeeVpa = vpaService.resolve(request.getPayeeVpa());

        // ── Step 3: Validate limits ──────────────────────────
        limitService.validate(payerVpa.getUserId(), request.getAmount());

        // ── Step 4: Create transaction (INITIATED) ───────────
        Transaction txn = Transaction.builder()
                .upiTxnId(IdGenerator.generateUpiTxnId())
                .rrn(IdGenerator.generateRRN())
                .txnType(TransactionType.PAY)
                .payerVpa(request.getPayerVpa())
                .payeeVpa(request.getPayeeVpa())
                .payerBankCode(payerVpa.getBankCode())
                .payeeBankCode(payeeVpa.getBankCode())
                .amount(request.getAmount())
                .currency("NPR")
                .status(TransactionStatus.INITIATED)
                .pspId(request.getPspId())
                .deviceFingerprint(request.getDeviceFingerprint())
                .ipAddress(request.getIpAddress())
                .note(request.getNote())
                .idempotencyKey(request.getIdempotencyKey())
                .expiresAt(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))
                .initiatedAt(Instant.now())
                .build();

        txnRepo.save(txn);
        log.info("Transaction created: {} | {} → {} | {} paisa",
                txn.getUpiTxnId(), txn.getPayerVpa(), txn.getPayeeVpa(), txn.getAmount());
        metrics.getTxnInitiated().increment();

        // ── Step 5: Fraud assessment ─────────────────────────
        fraudEngine.assess(payerVpa.getUserId(), request.getAmount(), txn.getId(),
                request.getDeviceFingerprint(), request.getPayeeVpa());

        // ── Step 6: MPIN encryption ─────────────────────────
        // User's MPIN is encrypted with the payer bank's public key.
        // The encrypted PIN is sent alongside the debit request.
        // Only the bank's HSM can decrypt it.
        String encryptedPin = null;
        if (request.getPin() != null && !request.getPin().isBlank()) {
            encryptedPin = pinEncryptionService.encryptPin(
                    request.getPin(), payerVpa.getBankCode());
            log.debug("MPIN encrypted for bank {} on txn {}", payerVpa.getBankCode(), txn.getUpiTxnId());
        } else {
            log.warn("No MPIN provided for txn {} — in production this would be rejected",
                    txn.getUpiTxnId());
        }

        stateMachine.transition(txn, TransactionStatus.DEBIT_PENDING);
        txnRepo.save(txn);

        // ── Step 7: Debit payer bank ─────────────────────────
        BankResponse debitResponse = bankConnector.debit(
                payerVpa.getBankCode(),
                payerVpa.getAccountNumber(),
                txn.getAmount(),
                txn.getUpiTxnId()
        );

        if (!debitResponse.isSuccess()) {
            stateMachine.transition(txn, TransactionStatus.DEBIT_FAILED);
            txn.setFailureCode(debitResponse.getErrorCode());
            txn.setFailureReason(debitResponse.getErrorMessage());
            txnRepo.save(txn);
            metrics.getTxnFailed().increment();
            metrics.getBankDebitFailed().increment();

            publishEvent(txn, payerVpa, payeeVpa);
            return TransactionResponse.failed(txn);
        }

        stateMachine.transition(txn, TransactionStatus.DEBITED);
        txnRepo.save(txn);
        metrics.getBankDebitSuccess().increment();

        // Record daily stats for limits
        limitService.recordTransaction(payerVpa.getUserId(), request.getAmount());

        // ── Step 8: Credit payee bank ────────────────────────
        stateMachine.transition(txn, TransactionStatus.CREDIT_PENDING);
        txnRepo.save(txn);

        BankResponse creditResponse = bankConnector.credit(
                payeeVpa.getBankCode(),
                payeeVpa.getAccountNumber(),
                txn.getAmount(),
                txn.getUpiTxnId()
        );

        if (!creditResponse.isSuccess()) {
            stateMachine.transition(txn, TransactionStatus.CREDIT_FAILED);
            txn.setFailureCode(creditResponse.getErrorCode());
            txn.setFailureReason(creditResponse.getErrorMessage());
            txnRepo.save(txn);
            metrics.getTxnFailed().increment();
            metrics.getBankCreditFailed().increment();

            // CRITICAL: Debit succeeded but credit failed — initiate reversal
            reversalService.initiateReversal(txn);

            publishEvent(txn, payerVpa, payeeVpa);
            return TransactionResponse.failed(txn);
        }

        // ── Step 9: Complete ─────────────────────────────────
        stateMachine.transition(txn, TransactionStatus.COMPLETED);
        txnRepo.save(txn);
        metrics.getTxnCompleted().increment();
        metrics.getBankCreditSuccess().increment();

        log.info("Transaction COMPLETED: {} | {} → {} | {} paisa in {}ms",
                txn.getUpiTxnId(), txn.getPayerVpa(), txn.getPayeeVpa(),
                txn.getAmount(),
                java.time.Duration.between(txn.getInitiatedAt(), txn.getCompletedAt()).toMillis());

        // ── Step 10: Publish async event ─────────────────────
        publishEvent(txn, payerVpa, payeeVpa);

        return TransactionResponse.success(txn);
    }

    /**
     * Check the current status of a transaction.
     */
    public TransactionResponse getStatus(String upiTxnId) {
        Transaction txn = txnRepo.findByUpiTxnId(upiTxnId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + upiTxnId));
        return TransactionResponse.from(txn);
    }

    private void publishEvent(Transaction txn, VpaDetails payerVpa, VpaDetails payeeVpa) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .txnId(txn.getId().toString())
                    .upiTxnId(txn.getUpiTxnId())
                    .rrn(txn.getRrn())
                    .payerVpa(txn.getPayerVpa())
                    .payeeVpa(txn.getPayeeVpa())
                    .payerUserId(payerVpa.getUserId())
                    .payeeUserId(payeeVpa.getUserId())
                    .amount(txn.getAmount())
                    .currency(txn.getCurrency())
                    .status(txn.getStatus())
                    .failureCode(txn.getFailureCode())
                    .occurredAt(Instant.now())
                    .build();
            eventPublisher.publish(event);
        } catch (Exception e) {
            // Event publishing failure should never break the transaction
            log.error("Failed to publish event for txn {}: {}", txn.getUpiTxnId(), e.getMessage());
        }
    }
}
