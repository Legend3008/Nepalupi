package np.com.nepalupi.domain.enums;

/**
 * ISO 8583 Response codes — Field 39.
 * <p>
 * When NCHL / bank responds to our message, field 39 tells us the outcome.
 * "00" is the only success code. Everything else is a decline or error.
 * These codes are standardized across the global banking system.
 */
public enum Iso8583ResponseCode {

    APPROVED("00", "Approved"),
    REFER_TO_ISSUER("01", "Refer to card issuer"),
    INVALID_MERCHANT("03", "Invalid merchant"),
    DO_NOT_HONOUR("05", "Do not honour"),
    ERROR("06", "Error"),
    INVALID_TRANSACTION("12", "Invalid transaction"),
    INVALID_AMOUNT("13", "Invalid amount"),
    INVALID_ACCOUNT("14", "Invalid account number"),
    NO_SUCH_ISSUER("15", "No such issuer"),
    FORMAT_ERROR("30", "Format error"),
    LOST_CARD("41", "Lost card"),
    STOLEN_CARD("43", "Stolen card"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    EXPIRED_CARD("54", "Expired card"),
    INCORRECT_PIN("55", "Incorrect PIN"),
    TXN_NOT_PERMITTED("57", "Transaction not permitted"),
    EXCEEDS_LIMIT("61", "Exceeds withdrawal limit"),
    RESTRICTED_CARD("62", "Restricted card"),
    PIN_TRIES_EXCEEDED("75", "PIN tries exceeded"),
    SYSTEM_MALFUNCTION("96", "System malfunction"),
    DUPLICATE_TRANSMISSION("94", "Duplicate transmission"),
    RECONCILE_ERROR("95", "Reconcile error");

    private final String code;
    private final String description;

    Iso8583ResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isApproved() {
        return this == APPROVED;
    }

    public static Iso8583ResponseCode fromCode(String code) {
        for (Iso8583ResponseCode rc : values()) {
            if (rc.code.equals(code)) {
                return rc;
            }
        }
        // Unknown code — treat as system malfunction
        return SYSTEM_MALFUNCTION;
    }
}
