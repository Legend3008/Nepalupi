package np.com.nepalupi.service.bank;

import np.com.nepalupi.domain.dto.response.BankResponse;

/**
 * Adapter interface for communicating with an individual bank's CBS
 * (Core Banking System) via NCHL or direct integration.
 * <p>
 * Each connected bank implements this interface.
 * In production, this sends ISO 8583 messages over a secure dedicated line.
 */
public interface BankAdapter {

    /**
     * @return the bank code this adapter handles (e.g., "NBBL" for Nabil Bank)
     */
    String getBankCode();

    /**
     * Debit (withdraw) from a bank account.
     */
    BankResponse debit(String accountNumber, Long amountPaisa, String txnId);

    /**
     * Credit (deposit) to a bank account.
     */
    BankResponse credit(String accountNumber, Long amountPaisa, String txnId);

    /**
     * Check balance of a bank account.
     */
    BankResponse checkBalance(String accountNumber);

    /**
     * Reverse a previously debited transaction.
     */
    BankResponse reversal(String originalTxnId);
}
