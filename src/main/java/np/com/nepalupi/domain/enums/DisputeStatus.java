package np.com.nepalupi.domain.enums;

/**
 * Dispute lifecycle status — 5 stages as per Nepal UPI spec.
 * <p>
 * Each stage has an SLA timer:
 * <ul>
 *   <li>RAISED → ACKNOWLEDGED: 24 hours</li>
 *   <li>UNDER_INVESTIGATION → RESOLVED: 3 business days (failed txn), 7 business days (fraud)</li>
 *   <li>RESOLVED → CLOSED: 24 hours (communicate to user)</li>
 * </ul>
 */
public enum DisputeStatus {

    /** User submitted the complaint */
    RAISED,

    /** System/ops acknowledged receipt */
    ACKNOWLEDGED,

    /** Team + banks investigating */
    UNDER_INVESTIGATION,

    /** Formal query sent to payer or payee bank via NCHL */
    AWAITING_BANK_RESPONSE,

    /** Outcome confirmed */
    RESOLVED,

    /** Resolution communicated and case archived */
    CLOSED;

    public boolean isTerminal() {
        return this == RESOLVED || this == CLOSED;
    }
}
