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
        // Simulate: Bank A customer pays Bank B customer Rs 1000 (100000 paisa)
        //           Bank B customer pays Bank C customer Rs 500 (50000 paisa)
        //           Bank A customer pays Bank C customer Rs 300 (30000 paisa)

        Transaction txn1 = buildTxn("BANKA", "BANKB", 100000L, TransactionStatus.COMPLETED);
        Transaction txn2 = buildTxn("BANKB", "BANKC", 50000L, TransactionStatus.COMPLETED);
        Transaction txn3 = buildTxn("BANKA", "BANKC", 30000L, TransactionStatus.COMPLETED);

        // Add a non-completed txn — should be ignored
        Transaction txn4 = buildTxn("BANKA", "BANKB", 200000L, TransactionStatus.DEBIT_FAILED);

        // Use reflection or direct instantiation — SettlementEngine.calculateNetPositions is package-private
        SettlementEngine engine = new SettlementEngine(null, null, null);
        Map<String, Long> positions = engine.calculateNetPositions(List.of(txn1, txn2, txn3, txn4));

        // Bank A: +100000 + 30000 = +130000 (owes switch 130000 paisa)
        assertEquals(130000L, positions.get("BANKA"));

        // Bank B: -100000 + 50000 = -50000 (owed 50000 by switch, net)
        assertEquals(-50000L, positions.get("BANKB"));

        // Bank C: -50000 - 30000 = -80000 (owed 80000 by switch)
        assertEquals(-80000L, positions.get("BANKC"));

        // Verify zero-sum: net of all positions must be 0
        long totalNet = positions.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(0L, totalNet, "Net positions must be zero-sum");
    }

    @Test
    @DisplayName("Empty transaction list produces empty positions")
    void emptyTransactions() {
        SettlementEngine engine = new SettlementEngine(null, null, null);
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
