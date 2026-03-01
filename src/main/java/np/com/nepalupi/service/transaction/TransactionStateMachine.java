package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.entity.TransactionAuditLog;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.exception.IllegalStateTransitionException;
import np.com.nepalupi.repository.TransactionAuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static np.com.nepalupi.domain.enums.TransactionStatus.*;

/**
 * Enforces the UPI transaction state machine.
 * <p>
 * NO code may set {@link Transaction#setStatus(TransactionStatus)} directly.
 * All transitions must go through this class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionStateMachine {

    private final TransactionAuditLogRepository auditLogRepository;

    /**
     * The canonical set of allowed state transitions.
     * Any transition not in this map is illegal and will throw.
     */
    private static final Map<TransactionStatus, Set<TransactionStatus>> VALID_TRANSITIONS = Map.of(
            INITIATED,          Set.of(DEBIT_PENDING, EXPIRED),
            DEBIT_PENDING,      Set.of(DEBITED, DEBIT_FAILED, EXPIRED),
            DEBITED,            Set.of(CREDIT_PENDING),
            CREDIT_PENDING,     Set.of(COMPLETED, CREDIT_FAILED),
            CREDIT_FAILED,      Set.of(REVERSAL_PENDING),
            REVERSAL_PENDING,   Set.of(REVERSED, REVERSAL_FAILED)
    );

    /**
     * Transition a transaction from its current status to a new status.
     *
     * @throws IllegalStateTransitionException if the transition is not allowed
     */
    public void transition(Transaction txn, TransactionStatus newStatus) {
        TransactionStatus currentStatus = txn.getStatus();

        Set<TransactionStatus> allowed = VALID_TRANSITIONS.get(currentStatus);

        if (allowed == null || !allowed.contains(newStatus)) {
            String msg = String.format(
                    "Illegal state transition for txn %s: %s → %s",
                    txn.getUpiTxnId(), currentStatus, newStatus
            );
            log.error(msg);
            throw new IllegalStateTransitionException(msg);
        }

        log.info("Txn {} state transition: {} → {}", txn.getUpiTxnId(), currentStatus, newStatus);

        txn.setStatus(newStatus);
        txn.setUpdatedAt(Instant.now());

        // Update timing fields based on the new status
        switch (newStatus) {
            case DEBITED -> txn.setDebitedAt(Instant.now());
            case COMPLETED -> {
                txn.setCreditedAt(Instant.now());
                txn.setCompletedAt(Instant.now());
            }
            default -> { /* no special timing update */ }
        }

        // Persist audit trail — every state change is logged permanently
        TransactionAuditLog auditLog = TransactionAuditLog.builder()
                .transactionId(txn.getId())
                .fromStatus(currentStatus != null ? currentStatus.name() : null)
                .toStatus(newStatus.name())
                .changedAt(Instant.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
