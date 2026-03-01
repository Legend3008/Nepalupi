package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Sweeps for transactions stuck in non-terminal states past their expiry.
 * Runs every minute.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionExpirySweeper {

    private final TransactionRepository txnRepo;
    private final TransactionStateMachine stateMachine;

    @Scheduled(fixedRate = 60_000)  // Every minute
    public void sweepExpiredTransactions() {
        Instant now = Instant.now();

        // Only expire INITIATED or DEBIT_PENDING transactions
        List<Transaction> expiredInitiated =
                txnRepo.findExpiredTransactions(TransactionStatus.INITIATED, now);
        List<Transaction> expiredDebitPending =
                txnRepo.findExpiredTransactions(TransactionStatus.DEBIT_PENDING, now);

        int count = 0;
        for (Transaction txn : expiredInitiated) {
            try {
                stateMachine.transition(txn, TransactionStatus.EXPIRED);
                txnRepo.save(txn);
                count++;
            } catch (Exception e) {
                log.error("Failed to expire txn {}: {}", txn.getUpiTxnId(), e.getMessage());
            }
        }

        for (Transaction txn : expiredDebitPending) {
            try {
                stateMachine.transition(txn, TransactionStatus.EXPIRED);
                txnRepo.save(txn);
                count++;
            } catch (Exception e) {
                log.error("Failed to expire txn {}: {}", txn.getUpiTxnId(), e.getMessage());
            }
        }

        if (count > 0) {
            log.info("Expired {} transactions", count);
        }
    }
}
