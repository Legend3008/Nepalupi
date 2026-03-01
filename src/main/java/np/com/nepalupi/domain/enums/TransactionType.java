package np.com.nepalupi.domain.enums;

/**
 * Type of UPI transaction.
 */
public enum TransactionType {

    /** Standard payer-initiated push payment (P2P or P2M). */
    PAY,

    /** Payee-initiated pull payment (collect request). */
    COLLECT,

    /** Reversal of a previously debited transaction. */
    REVERSAL,

    /** Mandate-based recurring payment. */
    MANDATE
}
