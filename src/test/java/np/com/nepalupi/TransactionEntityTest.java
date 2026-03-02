package np.com.nepalupi;

import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.domain.enums.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transaction Entity Tests")
class TransactionEntityTest {

    @Test
    @DisplayName("Transaction builder creates valid entity")
    void transactionBuilderWorks() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(600);

        Transaction txn = Transaction.builder()
                .id(id)
                .upiTxnId("NPL20260601120000abc123")
                .rrn("260601234567")
                .txnType(TransactionType.PAY)
                .payerVpa("ritesh@nchl")
                .payeeVpa("sita@nbbl")
                .payerBankCode("NCHL")
                .payeeBankCode("NBBL")
                .amount(150050L)
                .currency("NPR")
                .status(TransactionStatus.INITIATED)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(expiry)
                .build();

        assertEquals(id, txn.getId());
        assertEquals("NPL20260601120000abc123", txn.getUpiTxnId());
        assertEquals(TransactionType.PAY, txn.getTxnType());
        assertEquals("NPR", txn.getCurrency());
        assertEquals(150050L, txn.getAmount());
        assertEquals(TransactionStatus.INITIATED, txn.getStatus());
    }

    @Test
    @DisplayName("isExpired returns true for past expiry")
    void expiredTransactionDetected() {
        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID())
                .upiTxnId("NPL20260601120000def456")
                .rrn("260601234568")
                .payerVpa("user@nchl")
                .payeeVpa("user@nbbl")
                .payerBankCode("NCHL")
                .payeeBankCode("NBBL")
                .amount(10000L)
                .status(TransactionStatus.INITIATED)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(Instant.now().minusSeconds(60)) // Expired 1 minute ago
                .build();

        assertTrue(txn.isExpired());
    }

    @Test
    @DisplayName("isExpired returns false for future expiry")
    void nonExpiredTransactionDetected() {
        Transaction txn = Transaction.builder()
                .id(UUID.randomUUID())
                .upiTxnId("NPL20260601120000ghi789")
                .rrn("260601234569")
                .payerVpa("user@nchl")
                .payeeVpa("user@nbbl")
                .payerBankCode("NCHL")
                .payeeBankCode("NBBL")
                .amount(10000L)
                .status(TransactionStatus.INITIATED)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusSeconds(600)) // Expires in 10 min
                .build();

        assertFalse(txn.isExpired());
    }

    @Test
    @DisplayName("Terminal statuses correctly identified")
    void terminalStatuses() {
        assertTrue(TransactionStatus.COMPLETED.isTerminal());
        assertTrue(TransactionStatus.DEBIT_FAILED.isTerminal());
        assertTrue(TransactionStatus.REVERSED.isTerminal());
        assertTrue(TransactionStatus.REVERSAL_FAILED.isTerminal());
        assertTrue(TransactionStatus.EXPIRED.isTerminal());

        assertFalse(TransactionStatus.INITIATED.isTerminal());
        assertFalse(TransactionStatus.DEBIT_PENDING.isTerminal());
        assertFalse(TransactionStatus.DEBITED.isTerminal());
        assertFalse(TransactionStatus.CREDIT_PENDING.isTerminal());
        assertFalse(TransactionStatus.REVERSAL_PENDING.isTerminal());
    }

    @Test
    @DisplayName("All transaction types exist")
    void allTransactionTypes() {
        assertEquals(4, TransactionType.values().length);
        assertNotNull(TransactionType.PAY);
        assertNotNull(TransactionType.COLLECT);
        assertNotNull(TransactionType.REVERSAL);
        assertNotNull(TransactionType.MANDATE);
    }

    @Test
    @DisplayName("All transaction statuses exist")
    void allTransactionStatuses() {
        assertEquals(11, TransactionStatus.values().length);
    }
}
