package np.com.nepalupi;

import np.com.nepalupi.util.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ID Generator Tests")
class IdGeneratorTest {

    @Test
    @DisplayName("UPI Txn ID starts with NPL and has correct length")
    void upiTxnIdFormat() {
        String txnId = IdGenerator.generateUpiTxnId();
        assertTrue(txnId.startsWith("NPL"));
        assertEquals(29, txnId.length()); // NPL(3) + yyyyMMddHHmmss(14) + hex(12) = 29
    }

    @Test
    @DisplayName("UPI Txn IDs are unique")
    void upiTxnIdUniqueness() {
        String id1 = IdGenerator.generateUpiTxnId();
        String id2 = IdGenerator.generateUpiTxnId();
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("RRN has correct format")
    void rrnFormat() {
        String rrn = IdGenerator.generateRRN();
        assertEquals(12, rrn.length());
        // First 5 chars = YYDDD (year + day of year)
        assertTrue(rrn.matches("\\d{12}"));
    }

    @Test
    @DisplayName("STAN is 6 digits")
    void stanFormat() {
        String stan = IdGenerator.generateStan();
        assertEquals(6, stan.length());
        assertTrue(stan.matches("\\d{6}"));
    }
}
