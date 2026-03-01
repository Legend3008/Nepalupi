package np.com.nepalupi.domain.enums;

/**
 * Transaction status — the canonical state machine for every UPI transaction.
 * <p>
 * State transitions are enforced by {@link np.com.nepalupi.service.transaction.TransactionStateMachine}.
 * No code may set a transaction status directly; it must go through the state machine.
 */
public enum TransactionStatus {

    /** Payment just created, pending validation. */
    INITIATED,

    /** Debit request sent to payer bank, awaiting response. */
    DEBIT_PENDING,

    /** Payer bank confirmed debit. Money is out of payer account. */
    DEBITED,

    /** Payer bank rejected the debit (insufficient funds, wrong PIN, etc.). Terminal. */
    DEBIT_FAILED,

    /** Credit request sent to payee bank, awaiting response. */
    CREDIT_PENDING,

    /** Full cycle complete — money moved from payer to payee. Terminal. */
    COMPLETED,

    /** Credit to payee bank failed. Money debited but not credited → must reverse. */
    CREDIT_FAILED,

    /** Reversal request sent to payer bank to refund the debited amount. */
    REVERSAL_PENDING,

    /** Money successfully returned to payer after a credit failure. Terminal. */
    REVERSED,

    /** Reversal itself failed — requires manual ops intervention. Terminal. */
    REVERSAL_FAILED,

    /** Transaction was not completed within the allowed window. Terminal. */
    EXPIRED;

    public boolean isTerminal() {
        return this == COMPLETED
                || this == DEBIT_FAILED
                || this == REVERSED
                || this == REVERSAL_FAILED
                || this == EXPIRED;
    }
}
