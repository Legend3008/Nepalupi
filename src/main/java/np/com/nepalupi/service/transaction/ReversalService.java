package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.dto.response.BankResponse;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.service.bank.BankConnector;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Handles reversals when a credit fails after a successful debit.
 * <p>
 * This is the most critical error-handling path in UPI:
 * - Money was debited from payer
 * - Credit to payee failed
 * - We MUST return the money to the payer
 * <p>
 * If the reversal itself fails, escalate to ops team immediately.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReversalService {

    private final TransactionStateMachine stateMachine;
    private final TransactionRepository txnRepo;
    private final BankConnector bankConnector;

    /**
     * Initiate reversal of a debited transaction.
     * Runs asynchronously to not block the main transaction response.
     */
    @Async
    public void initiateReversal(Transaction txn) {
        log.warn("REVERSAL INITIATED for txn {}: debit succeeded but credit failed",
                txn.getUpiTxnId());

        try {
            stateMachine.transition(txn, TransactionStatus.REVERSAL_PENDING);
            txnRepo.save(txn);

            BankResponse reversalResponse = bankConnector.reversal(
                    txn.getPayerBankCode(),
                    txn.getUpiTxnId()
            );

            if (reversalResponse.isSuccess()) {
                stateMachine.transition(txn, TransactionStatus.REVERSED);
                txnRepo.save(txn);
                log.info("REVERSAL SUCCESS for txn {}", txn.getUpiTxnId());
            } else {
                stateMachine.transition(txn, TransactionStatus.REVERSAL_FAILED);
                txn.setFailureCode("REVERSAL_" + reversalResponse.getErrorCode());
                txn.setFailureReason("Reversal failed: " + reversalResponse.getErrorMessage());
                txnRepo.save(txn);

                // CRITICAL: Escalate to ops team — money is stuck
                log.error("REVERSAL FAILED for txn {} — ESCALATING TO OPS TEAM. " +
                                "Amount: {} paisa, Payer: {}, PayerBank: {}",
                        txn.getUpiTxnId(), txn.getAmount(),
                        txn.getPayerVpa(), txn.getPayerBankCode());

                // In production: send alert to PagerDuty / ops Slack channel
            }
        } catch (Exception e) {
            log.error("REVERSAL EXCEPTION for txn {} — MANUAL INTERVENTION REQUIRED",
                    txn.getUpiTxnId(), e);
        }
    }
}
