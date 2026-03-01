package np.com.nepalupi.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates globally unique IDs for UPI transactions.
 * <p>
 * Format follows India's NPCI convention adapted for Nepal:
 * - UPI Txn ID:  NPL + yyyyMMddHHmmss + 12-char random hex  (35 chars)
 * - RRN:         yyDDD + 7-digit sequence (12 chars, like NPCI's RRN)
 */
@Component
public class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TXN_ID_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Kathmandu"));
    private static final DateTimeFormatter RRN_FMT =
            DateTimeFormatter.ofPattern("yyDDD").withZone(ZoneId.of("Asia/Kathmandu"));

    /**
     * Generate a globally unique UPI transaction ID.
     * Example: NPL20260301143022a1b2c3d4e5f6
     */
    public static String generateUpiTxnId() {
        String timestamp = TXN_ID_FMT.format(Instant.now());
        String random = randomHex(12);
        return "NPL" + timestamp + random;
    }

    /**
     * Generate a Reference Retrieval Number (RRN).
     * Format: YYDDD + 7-digit random (like NPCI's 12-digit RRN).
     */
    public static String generateRRN() {
        String dayOfYear = RRN_FMT.format(Instant.now());
        int seq = RANDOM.nextInt(9_999_999) + 1;
        return dayOfYear + String.format("%07d", seq);
    }

    /**
     * Generate a System Trace Audit Number (STAN) for ISO 8583 messages.
     */
    public static String generateStan() {
        int stan = RANDOM.nextInt(999_999) + 1;
        return String.format("%06d", stan);
    }

    /**
     * Generate a default idempotency key if none provided.
     */
    public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    private static String randomHex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(RANDOM.nextInt(16)));
        }
        return sb.toString();
    }
}
