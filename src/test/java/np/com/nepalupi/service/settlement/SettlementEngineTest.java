package np.com.nepalupi.service.settlement;

import np.com.nepalupi.service.settlement.SettlementEngine;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Settlement Engine Tests")
class SettlementEngineTest {

    @Test
    @DisplayName("Net positions calculated correctly for completed transactions")
    void netPositionsCalculation() {
        Transaction txn1 = buildTxn("BANKA", "BANKB", 100000L, TransactionStatus.COMPLETED);
        Transaction txn2 = buildTxn("BANKB", "BANKC", 50000L, TransactionStatus.COMPLETED);
        Transaction txn3 = buildTxn("BANKA", "BANKC", 30000L, TransactionStatus.COMPLETED);
        Transaction txn4 = buildTxn("BANKA", "BANKB", 200000L, TransactionStatus.DEBIT_FAILED);

        SettlementEngine engine = new SettlementEngine(null, null, null, null, null, null);
        Map<String, Long> positions = engine.calculateNetPositions(List.of(txn1, txn2, txn3, txn4));

        assertEquals(130000L, positions.get("BANKA"));
        assertEquals(-50000L, positions.get("BANKB"));
        assertEquals(-80000L, positions.get("BANKC"));

        long totalNet = positions.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(0L, totalNet, "Net positions must be zero-sum");
    }

    @Test
    @DisplayName("Empty transaction list produces empty positions")
    void emptyTransactions() {
        SettlementEngine engine = new SettlementEngine(null, null, null, null, null, null);
        Map<String, Long> positions = engine.calculateNetPositions(List.of());
        assertTrue(positions.isEmpty());
    }

    private Transaction buildTxn(String payerBank, String payeeBank, Long amount, TransactionStatus status) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .upiTxnId("NPL" + UUID.randomUUID().toString().substring(0, 12))
                .rrn("260601234567")
                .payerVpa("user@" + payerBank.toLowerCase())
                .payeeVpa("user@" + payeeBank.toLowerCase())
                .payerBankCode(payerBank)
                .payeeBankCode(payeeBank)
                .amount(amount)
                .status(status)
                .idempotencyKey(UUID.randomUUID().toString())
                .expiresAt(java.time.Instant.now().plusSeconds(600))
                .build();
    }
}
