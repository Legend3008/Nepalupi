package np.com.nepalupi;

import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.exception.IllegalStateTransitionException;
import np.com.nepalupi.repository.TransactionAuditLogRepository;
import np.com.nepalupi.service.transaction.TransactionStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static np.com.nepalupi.domain.enums.TransactionStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction State Machine Tests")
class TransactionStateMachineTest {

    @Mock
    private TransactionAuditLogRepository auditLogRepository;

    private TransactionStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TransactionStateMachine(auditLogRepository);
    }

    private Transaction createTxn(TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .upiTxnId("NPL20260301143022abc123")
                .rrn("260601234567")
                .payerVpa("ritesh@nchl")
                .payeeVpa("sita@nchl")
                .payerBankCode("NBBL")
                .payeeBankCode("NIBL")
                .amount(150050L)
                .status(status)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
    }

    @Test
    @DisplayName("INITIATED → DEBIT_PENDING is valid")
    void initiatedToDebitPending() {
        Transaction txn = createTxn(INITIATED);
        stateMachine.transition(txn, DEBIT_PENDING);
        assertEquals(DEBIT_PENDING, txn.getStatus());
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("INITIATED → EXPIRED is valid")
    void initiatedToExpired() {
        Transaction txn = createTxn(INITIATED);
        stateMachine.transition(txn, EXPIRED);
        assertEquals(EXPIRED, txn.getStatus());
    }

    @Test
    @DisplayName("DEBIT_PENDING → DEBITED is valid")
    void debitPendingToDebited() {
        Transaction txn = createTxn(DEBIT_PENDING);
        stateMachine.transition(txn, DEBITED);
        assertEquals(DEBITED, txn.getStatus());
        assertNotNull(txn.getDebitedAt());
    }

    @Test
    @DisplayName("DEBIT_PENDING → DEBIT_FAILED is valid")
    void debitPendingToDebitFailed() {
        Transaction txn = createTxn(DEBIT_PENDING);
        stateMachine.transition(txn, DEBIT_FAILED);
        assertEquals(DEBIT_FAILED, txn.getStatus());
    }

    @Test
    @DisplayName("DEBITED → CREDIT_PENDING is valid")
    void debitedToCreditPending() {
        Transaction txn = createTxn(DEBITED);
        stateMachine.transition(txn, CREDIT_PENDING);
        assertEquals(CREDIT_PENDING, txn.getStatus());
    }

    @Test
    @DisplayName("CREDIT_PENDING → COMPLETED is valid")
    void creditPendingToCompleted() {
        Transaction txn = createTxn(CREDIT_PENDING);
        stateMachine.transition(txn, COMPLETED);
        assertEquals(COMPLETED, txn.getStatus());
        assertNotNull(txn.getCompletedAt());
        assertNotNull(txn.getCreditedAt());
    }

    @Test
    @DisplayName("CREDIT_PENDING → CREDIT_FAILED is valid")
    void creditPendingToCreditFailed() {
        Transaction txn = createTxn(CREDIT_PENDING);
        stateMachine.transition(txn, CREDIT_FAILED);
        assertEquals(CREDIT_FAILED, txn.getStatus());
    }

    @Test
    @DisplayName("CREDIT_FAILED → REVERSAL_PENDING is valid")
    void creditFailedToReversalPending() {
        Transaction txn = createTxn(CREDIT_FAILED);
        stateMachine.transition(txn, REVERSAL_PENDING);
        assertEquals(REVERSAL_PENDING, txn.getStatus());
    }

    @Test
    @DisplayName("REVERSAL_PENDING → REVERSED is valid")
    void reversalPendingToReversed() {
        Transaction txn = createTxn(REVERSAL_PENDING);
        stateMachine.transition(txn, REVERSED);
        assertEquals(REVERSED, txn.getStatus());
    }

    @Test
    @DisplayName("REVERSAL_PENDING → REVERSAL_FAILED is valid")
    void reversalPendingToReversalFailed() {
        Transaction txn = createTxn(REVERSAL_PENDING);
        stateMachine.transition(txn, REVERSAL_FAILED);
        assertEquals(REVERSAL_FAILED, txn.getStatus());
    }

    // ── Invalid transitions ──────────────────────────────────

    @Test
    @DisplayName("INITIATED → COMPLETED should throw")
    void initiatedToCompletedThrows() {
        Transaction txn = createTxn(INITIATED);
        assertThrows(IllegalStateTransitionException.class,
                () -> stateMachine.transition(txn, COMPLETED));
    }

    @Test
    @DisplayName("COMPLETED → DEBIT_PENDING should throw (terminal state)")
    void completedToDebitPendingThrows() {
        Transaction txn = createTxn(COMPLETED);
        assertThrows(IllegalStateTransitionException.class,
                () -> stateMachine.transition(txn, DEBIT_PENDING));
    }

    @Test
    @DisplayName("DEBIT_FAILED → CREDIT_PENDING should throw (terminal state)")
    void debitFailedToCreditPendingThrows() {
        Transaction txn = createTxn(DEBIT_FAILED);
        assertThrows(IllegalStateTransitionException.class,
                () -> stateMachine.transition(txn, CREDIT_PENDING));
    }

    @Test
    @DisplayName("DEBITED → DEBIT_FAILED should throw (can't go backward)")
    void debitedToDebitFailedThrows() {
        Transaction txn = createTxn(DEBITED);
        assertThrows(IllegalStateTransitionException.class,
                () -> stateMachine.transition(txn, DEBIT_FAILED));
    }

    @Test
    @DisplayName("Full happy path: INITIATED → COMPLETED")
    void fullHappyPath() {
        Transaction txn = createTxn(INITIATED);

        stateMachine.transition(txn, DEBIT_PENDING);
        assertEquals(DEBIT_PENDING, txn.getStatus());

        stateMachine.transition(txn, DEBITED);
        assertEquals(DEBITED, txn.getStatus());
        assertNotNull(txn.getDebitedAt());

        stateMachine.transition(txn, CREDIT_PENDING);
        assertEquals(CREDIT_PENDING, txn.getStatus());

        stateMachine.transition(txn, COMPLETED);
        assertEquals(COMPLETED, txn.getStatus());
        assertNotNull(txn.getCompletedAt());
    }

    @Test
    @DisplayName("Full reversal path: credit failure → reversal")
    void fullReversalPath() {
        Transaction txn = createTxn(CREDIT_PENDING);

        stateMachine.transition(txn, CREDIT_FAILED);
        assertEquals(CREDIT_FAILED, txn.getStatus());

        stateMachine.transition(txn, REVERSAL_PENDING);
        assertEquals(REVERSAL_PENDING, txn.getStatus());

        stateMachine.transition(txn, REVERSED);
        assertEquals(REVERSED, txn.getStatus());
    }
}
