package np.com.nepalupi.domain.enums;

/**
 * Dispute types — categorized as per NPCI UPI Circular model.
 */
public enum DisputeType {

    /** Money debited from payer but never credited to payee */
    DEBIT_WITHOUT_CREDIT,

    /** Transaction shows failed/pending but payer's money was debited */
    FAILED_BUT_DEBITED,

    /** User claims they didn't initiate this transaction — fraud claim */
    UNAUTHORIZED_TRANSACTION,

    /** User was charged twice for a single transaction */
    DUPLICATE_CHARGE,

    /** Other dispute not fitting above categories */
    OTHER;
}
