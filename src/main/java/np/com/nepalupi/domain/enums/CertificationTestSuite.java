package np.com.nepalupi.domain.enums;

/**
 * Certification test suites that every PSP must pass before
 * receiving production credentials.
 */
public enum CertificationTestSuite {

    VPA_RESOLUTION("VPA Resolution", true),
    PAYMENT_INITIATION("Payment Initiation", true),
    PIN_FLOW("PIN Flow", true),
    STATUS_QUERY("Transaction Status Query", true),
    WEBHOOK_RECEIPT("Webhook Receipt & Acknowledgment", true),
    ERROR_HANDLING("Error Handling for all failure scenarios", true),
    REVERSAL_HANDLING("Reversal Handling", false),
    COLLECT_FLOW("Collect Request Flow", false),
    RATE_LIMITING("Rate Limiting Compliance", false),
    CERTIFICATE_PINNING("Certificate Pinning in Mobile App", false);

    private final String description;
    private final boolean mandatory;

    CertificationTestSuite(String description, boolean mandatory) {
        this.description = description;
        this.mandatory = mandatory;
    }

    public String getDescription() { return description; }
    public boolean isMandatory() { return mandatory; }
}
