package np.com.nepalupi.domain.enums;

/**
 * Fraud signals detected by the fraud engine.
 */
public enum FraudSignal {

    /** Transaction amount is ≥ 5x the user's 30-day average. */
    AMOUNT_SPIKE,

    /** More than 5 transactions within the last hour. */
    HIGH_VELOCITY,

    /** High-value transaction from a device not previously seen. */
    NEW_DEVICE_HIGH_AMOUNT,

    /** Transaction to a newly created VPA (< 24 hours old). */
    NEW_PAYEE_VPA,

    /** Multiple failed PIN attempts before a successful one. */
    PIN_BRUTE_FORCE
}
