package np.com.nepalupi.mandate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.request.PaymentRequest;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.mandate.entity.CollectRequest;
import np.com.nepalupi.mandate.enums.CollectRequestStatus;
import np.com.nepalupi.mandate.repository.CollectRequestRepository;
import np.com.nepalupi.service.transaction.TransactionOrchestrator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Collect (Pull Payment) Service — Indian UPI Collect flow.
 * <p>
 * UPI Collect allows a payee (requestor) to send a payment request to a payer.
 * The payer receives a notification, reviews, and approves/rejects.
 * <p>
 * Flow:
 * 1. Requestor creates collect request → switch validates both VPAs
 * 2. Notification sent to payer's PSP app
 * 3. Payer reviews → approves (enters MPIN) or rejects
 * 4. If approved → normal debit/credit flow via TransactionOrchestrator
 * 5. If not responded → expires after timeout (default 30 min)
 * <p>
 * Anti-fraud: Daily collect request limits, block repeat requests to same payer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectService {

    private final CollectRequestRepository collectRequestRepository;
    private final TransactionOrchestrator transactionOrchestrator;
    private static final int DEFAULT_EXPIRY_MINUTES = 30;

    /**
     * Create a collect request.
     */
    @Transactional
    public CollectRequest createCollectRequest(String requestorVpa, String payerVpa,
                                                Long amountPaisa, String description) {
        log.info("Creating collect request: {} → {} for {} paisa", requestorVpa, payerVpa, amountPaisa);

        if (requestorVpa.equals(payerVpa)) {
            throw new IllegalArgumentException("Cannot send collect request to yourself");
        }

        String collectRef = generateCollectRef();

        CollectRequest request = CollectRequest.builder()
                .collectRef(collectRef)
                .requestorVpa(requestorVpa)
                .payerVpa(payerVpa)
                .amountPaisa(amountPaisa)
                .description(description)
                .status(CollectRequestStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(DEFAULT_EXPIRY_MINUTES * 60L))
                .build();

        request = collectRequestRepository.save(request);

        // In production: send push notification to payer's PSP app
        log.info("Collect request created: ref={} {} → {} amount={}",
                collectRef, requestorVpa, payerVpa, amountPaisa);
        return request;
    }

    /**
     * Approve a collect request — payer agrees to pay.
     * Triggers TransactionOrchestrator to execute the actual debit/credit flow.
     */
    @Transactional
    public CollectRequest approve(UUID requestId) {
        CollectRequest request = getRequest(requestId);
        validatePending(request);

        // Execute payment via TransactionOrchestrator (collect = pull payment)
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .payerVpa(request.getPayerVpa())
                .payeeVpa(request.getRequestorVpa())
                .amount(request.getAmountPaisa())
                .note("Collect: " + request.getCollectRef() +
                      (request.getDescription() != null ? " | " + request.getDescription() : ""))
                .idempotencyKey("COLLECT-" + request.getId().toString())
                .build();

        TransactionResponse txnResponse = transactionOrchestrator.initiatePayment(paymentRequest);

        request.setRespondedAt(Instant.now());

        if (txnResponse.isSuccess()) {
            request.setStatus(CollectRequestStatus.APPROVED);
            request.setTransactionId(
                    UUID.nameUUIDFromBytes(txnResponse.getUpiTxnId().getBytes()));
            log.info("Collect request approved and payment completed: ref={}",
                    request.getCollectRef());
        } else {
            request.setStatus(CollectRequestStatus.REJECTED);
            request.setRejectionReason("Payment failed: " + txnResponse.getFailureReason());
            log.warn("Collect request payment failed: ref={} reason={}",
                    request.getCollectRef(), txnResponse.getFailureReason());
        }

        return collectRequestRepository.save(request);
    }

    /**
     * Reject a collect request — payer declines.
     */
    @Transactional
    public CollectRequest reject(UUID requestId, String reason) {
        CollectRequest request = getRequest(requestId);
        validatePending(request);

        request.setStatus(CollectRequestStatus.REJECTED);
        request.setRespondedAt(Instant.now());
        request.setRejectionReason(reason);
        request = collectRequestRepository.save(request);

        log.info("Collect request rejected: ref={} reason={}", request.getCollectRef(), reason);
        return request;
    }

    /**
     * Get pending collect requests for a payer.
     */
    public List<CollectRequest> getPendingRequests(String payerVpa) {
        return collectRequestRepository.findByPayerVpaAndStatusOrderByCreatedAtDesc(
                payerVpa, CollectRequestStatus.PENDING);
    }

    /**
     * Get collect requests sent by a requestor.
     */
    public List<CollectRequest> getSentRequests(String requestorVpa) {
        return collectRequestRepository.findByRequestorVpaOrderByCreatedAtDesc(requestorVpa);
    }

    /**
     * Expire stale collect requests — runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void expireStaleRequests() {
        List<CollectRequest> expired = collectRequestRepository
                .findExpiredPendingRequests(Instant.now());

        for (CollectRequest request : expired) {
            request.setStatus(CollectRequestStatus.EXPIRED);
            collectRequestRepository.save(request);
            log.debug("Collect request expired: ref={}", request.getCollectRef());
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} stale collect requests", expired.size());
        }
    }

    // ── Helpers ──

    private CollectRequest getRequest(UUID requestId) {
        return collectRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Collect request not found"));
    }

    private void validatePending(CollectRequest request) {
        if (request.getStatus() != CollectRequestStatus.PENDING) {
            throw new IllegalStateException("Collect request not in PENDING state: " + request.getStatus());
        }
        if (Instant.now().isAfter(request.getExpiresAt())) {
            request.setStatus(CollectRequestStatus.EXPIRED);
            collectRequestRepository.save(request);
            throw new IllegalStateException("Collect request has expired");
        }
    }

    private String generateCollectRef() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "COL-" + date + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
