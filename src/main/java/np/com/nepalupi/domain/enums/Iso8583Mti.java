package np.com.nepalupi.domain.enums;

/**
 * ISO 8583 Message Type Indicator (MTI).
 * <p>
 * Every ISO 8583 message starts with a 4-digit MTI that tells the receiver
 * what kind of message this is. Same codes used by NPCI in Indian UPI.
 */
public enum Iso8583Mti {

    /** Financial transaction request (debit / credit / balance) */
    MTI_0200("0200", "Financial Request"),

    /** Financial transaction response from bank */
    MTI_0210("0210", "Financial Response"),

    /** Reversal request */
    MTI_0400("0400", "Reversal Request"),

    /** Reversal response */
    MTI_0410("0410", "Reversal Response"),

    /** Network management (sign-on, heartbeat, echo test) */
    MTI_0800("0800", "Network Management Request"),

    /** Network management response */
    MTI_0810("0810", "Network Management Response");

    private final String code;
    private final String description;

    Iso8583Mti(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static Iso8583Mti fromCode(String code) {
        for (Iso8583Mti mti : values()) {
            if (mti.code.equals(code)) {
                return mti;
            }
        }
        throw new IllegalArgumentException("Unknown MTI code: " + code);
    }
}
