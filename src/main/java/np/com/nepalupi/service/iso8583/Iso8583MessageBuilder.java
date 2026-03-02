package np.com.nepalupi.service.iso8583;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.config.NchlConfig;
import np.com.nepalupi.util.IdGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Builds ISO 8583 messages for NCHL communication.
 * <p>
 * Uses jPOS ISOMsg under the hood. Each message is a structured form with
 * specific fields at specific positions. Think of it like filling out a
 * government form — every field has a fixed position and meaning.
 * <p>
 * Field reference (key fields for Nepal UPI):
 * <ul>
 *   <li>F2  — Account number (PAN)</li>
 *   <li>F3  — Processing code (000000=debit, 200000=credit, 400000=reversal)</li>
 *   <li>F4  — Amount in smallest unit (paisa), 12-digit zero-padded</li>
 *   <li>F7  — Transmission date/time (MMddHHmmss)</li>
 *   <li>F11 — STAN (System Trace Audit Number, 6-digit)</li>
 *   <li>F12 — Local time (HHmmss)</li>
 *   <li>F13 — Local date (MMdd)</li>
 *   <li>F37 — RRN (Retrieval Reference Number)</li>
 *   <li>F38 — Authorization code (in response)</li>
 *   <li>F39 — Response code ("00" = approved)</li>
 *   <li>F41 — Terminal ID</li>
 *   <li>F42 — Acquirer/Merchant ID</li>
 *   <li>F49 — Currency code (524 = NPR)</li>
 *   <li>F55 — Encrypted PIN block</li>
 *   <li>F90 — Original data element (for reversals)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Iso8583MessageBuilder {

    private static final ZoneId NEPAL_ZONE = ZoneId.of("Asia/Kathmandu");
    private static final String CURRENCY_NPR = "524";
    private static final DateTimeFormatter TRANSMISSION_DT = DateTimeFormatter.ofPattern("MMddHHmmss");
    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter LOCAL_DATE = DateTimeFormatter.ofPattern("MMdd");

    private final NchlConfig nchlConfig;

    /**
     * Build a 0200 Financial Request for debit.
     *
     * @param accountNumber payer's bank account
     * @param amountPaisa   amount in paisa (Rs 1500 = 150000)
     * @param rrn           our RRN for matching
     * @param stan          STAN for this message
     * @param pinBlock      encrypted PIN block (field 55), nullable for credit
     * @return structured ISO 8583 message fields
     */
    public Iso8583Message buildDebitRequest(String accountNumber, long amountPaisa,
                                            String rrn, String stan, String pinBlock) {
        return buildFinancialRequest(accountNumber, amountPaisa, "000000", rrn, stan, pinBlock);
    }

    /**
     * Build a 0200 Financial Request for credit.
     * No PIN required for credit — we are pushing money, not pulling.
     */
    public Iso8583Message buildCreditRequest(String accountNumber, long amountPaisa,
                                              String rrn, String stan) {
        return buildFinancialRequest(accountNumber, amountPaisa, "200000", rrn, stan, null);
    }

    /**
     * Build a 0200 Balance Inquiry Request.
     * Processing code: 310000 (balance inquiry).
     * Amount field is zero for balance inquiries.
     *
     * @param accountNumber account to check balance for
     * @param rrn           our RRN for matching
     * @param stan          STAN for this message
     * @return structured ISO 8583 balance inquiry message
     */
    public Iso8583Message buildBalanceInquiryRequest(String accountNumber, String rrn, String stan) {
        LocalDateTime now = LocalDateTime.now(NEPAL_ZONE);

        return Iso8583Message.builder()
                .mti("0200")
                .field2(accountNumber)
                .field3("310000")  // balance inquiry processing code
                .field4(formatAmount(0))
                .field7(now.format(TRANSMISSION_DT))
                .field11(stan)
                .field12(now.format(LOCAL_TIME))
                .field13(now.format(LOCAL_DATE))
                .field37(rrn)
                .field41(nchlConfig.getTerminalId())
                .field42(nchlConfig.getAcquirerId())
                .field49(CURRENCY_NPR)
                .build();
    }

    /**
     * Build a 0400 Reversal Request.
     *
     * @param originalRrn   RRN of the original transaction being reversed
     * @param originalStan  STAN of the original transaction
     * @param accountNumber account to reverse
     * @param amountPaisa   amount to reverse
     */
    public Iso8583Message buildReversalRequest(String accountNumber, long amountPaisa,
                                                String originalRrn, String originalStan,
                                                String newStan) {
        LocalDateTime now = LocalDateTime.now(NEPAL_ZONE);

        return Iso8583Message.builder()
                .mti("0400")
                .field2(accountNumber)
                .field3("400000")  // reversal processing code
                .field4(formatAmount(amountPaisa))
                .field7(now.format(TRANSMISSION_DT))
                .field11(newStan)
                .field12(now.format(LOCAL_TIME))
                .field13(now.format(LOCAL_DATE))
                .field37(originalRrn)
                .field41(nchlConfig.getTerminalId())
                .field42(nchlConfig.getAcquirerId())
                .field49(CURRENCY_NPR)
                .field90(buildOriginalDataElement(originalRrn, originalStan, amountPaisa))
                .build();
    }

    /**
     * Build a 0800 Sign-On request.
     * Must be sent before any financial messages after connection/reconnection.
     */
    public Iso8583Message buildSignOnRequest(String stan) {
        LocalDateTime now = LocalDateTime.now(NEPAL_ZONE);

        return Iso8583Message.builder()
                .mti("0800")
                .field7(now.format(TRANSMISSION_DT))
                .field11(stan)
                .field12(now.format(LOCAL_TIME))
                .field13(now.format(LOCAL_DATE))
                .field70("001")  // Network management information code: sign-on
                .build();
    }

    /**
     * Build a 0800 Heartbeat/Echo request.
     * Sent every 60 seconds to keep TCP alive and verify NCHL is reachable.
     */
    public Iso8583Message buildHeartbeatRequest(String stan) {
        LocalDateTime now = LocalDateTime.now(NEPAL_ZONE);

        return Iso8583Message.builder()
                .mti("0800")
                .field7(now.format(TRANSMISSION_DT))
                .field11(stan)
                .field12(now.format(LOCAL_TIME))
                .field13(now.format(LOCAL_DATE))
                .field70("301")  // Network management information code: echo test
                .build();
    }

    // ── Private helpers ──────────────────────────────────────

    private Iso8583Message buildFinancialRequest(String accountNumber, long amountPaisa,
                                                  String processingCode, String rrn,
                                                  String stan, String pinBlock) {
        LocalDateTime now = LocalDateTime.now(NEPAL_ZONE);

        Iso8583Message.Iso8583MsgBuilder builder = Iso8583Message.builder()
                .mti("0200")
                .field2(accountNumber)
                .field3(processingCode)
                .field4(formatAmount(amountPaisa))
                .field7(now.format(TRANSMISSION_DT))
                .field11(stan)
                .field12(now.format(LOCAL_TIME))
                .field13(now.format(LOCAL_DATE))
                .field37(rrn)
                .field41(nchlConfig.getTerminalId())
                .field42(nchlConfig.getAcquirerId())
                .field49(CURRENCY_NPR);

        if (pinBlock != null) {
            builder.field55(pinBlock);
        }

        return builder.build();
    }

    /**
     * Format amount in paisa as 12-digit zero-padded string.
     * Rs 1500 = 150000 paisa → "000000150000"
     */
    private String formatAmount(long amountPaisa) {
        return String.format("%012d", amountPaisa);
    }

    /**
     * Field 90: Original Data Element for reversals.
     * Contains original MTI + STAN + date/time + acquirer/forwarding.
     */
    private String buildOriginalDataElement(String originalRrn, String originalStan, long amountPaisa) {
        return "0200" + originalStan + LocalDateTime.now(NEPAL_ZONE).format(TRANSMISSION_DT)
                + String.format("%012d", amountPaisa);
    }

    /**
     * Structured representation of an ISO 8583 message (simplified for our use).
     * In production, this maps directly to jPOS ISOMsg fields.
     */
    @lombok.Builder(builderClassName = "Iso8583MsgBuilder")
    @lombok.Getter
    public static class Iso8583Message {
        private String mti;
        private String field2;    // Account number / PAN
        private String field3;    // Processing code
        private String field4;    // Amount
        private String field7;    // Transmission date/time
        private String field11;   // STAN
        private String field12;   // Local time
        private String field13;   // Local date
        private String field37;   // RRN
        private String field38;   // Auth code (response)
        private String field39;   // Response code (response)
        private String field41;   // Terminal ID
        private String field42;   // Acquirer ID
        private String field49;   // Currency code
        private String field55;   // PIN block
        private String field64;   // Primary MAC (Message Authentication Code)
        private String field70;   // Network mgmt info code
        private String field90;   // Original data element (reversals)
        private String field128;  // Secondary MAC

        /**
         * Convert to hex dump for logging (simplified).
         */
        public String toHexDump() {
            StringBuilder sb = new StringBuilder();
            sb.append("MTI=").append(mti);
            if (field2 != null) sb.append("|F2=").append(maskAccount(field2));
            if (field3 != null) sb.append("|F3=").append(field3);
            if (field4 != null) sb.append("|F4=").append(field4);
            if (field11 != null) sb.append("|F11=").append(field11);
            if (field37 != null) sb.append("|F37=").append(field37);
            if (field39 != null) sb.append("|F39=").append(field39);
            if (field49 != null) sb.append("|F49=").append(field49);
            if (field70 != null) sb.append("|F70=").append(field70);
            return sb.toString();
        }

        private String maskAccount(String account) {
            if (account == null || account.length() < 8) return "****";
            return account.substring(0, 4) + "****" + account.substring(account.length() - 4);
        }
    }
}
