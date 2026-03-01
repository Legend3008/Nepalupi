package np.com.nepalupi.domain.enums;

/**
 * Actions that can be taken on a dispute.
 */
public enum DisputeAction {
    RAISED,
    ACKNOWLEDGED,
    BANK_QUERY_SENT,
    BANK_RESPONSE_RECEIVED,
    AUTO_RESOLVED,
    MANUAL_ESCALATED,
    ESCALATED_TO_NCHL,
    ESCALATED_TO_NRB,
    REFUND_INITIATED,
    REFUND_COMPLETED,
    FRAUD_REPORTED,
    RESOLVED,
    CLOSED
}
