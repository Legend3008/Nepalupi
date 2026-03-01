package np.com.nepalupi.service.bank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.response.BankResponse;
import np.com.nepalupi.exception.BankNotSupportedException;
import np.com.nepalupi.exception.BankTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central connector that routes bank operations to the correct {@link BankAdapter}.
 * <p>
 * In the NCHL model, all banks are reached via the NCHL switch.
 * Each bank can optionally have a direct adapter; otherwise the NCHL default is used.
 * <p>
 * Includes retry with exponential backoff for transient bank timeouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankConnector {

    private final List<BankAdapter> adapterList;
    private final Map<String, BankAdapter> adapters = new HashMap<>();
    private BankAdapter defaultAdapter;

    @PostConstruct
    void init() {
        for (BankAdapter adapter : adapterList) {
            adapters.put(adapter.getBankCode(), adapter);
            if ("NCHL".equals(adapter.getBankCode())) {
                defaultAdapter = adapter;
            }
        }
        log.info("Registered {} bank adapters. Default: NCHL", adapters.size());
    }

    /**
     * Send a debit request to the payer's bank.
     * Retries up to 3 times with exponential backoff on timeout.
     */
    @Retryable(
            retryFor = BankTimeoutException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public BankResponse debit(String bankCode, String accountNumber,
                               Long amountPaisa, String txnId) {
        BankAdapter adapter = resolveAdapter(bankCode);
        log.info("Sending DEBIT to bank {} for txn {}", bankCode, txnId);

        try {
            return adapter.debit(accountNumber, amountPaisa, txnId);
        } catch (Exception e) {
            log.warn("Bank timeout during DEBIT for txn {}: {}", txnId, e.getMessage());
            throw new BankTimeoutException("Debit timeout for bank " + bankCode, e);
        }
    }

    /**
     * Send a credit request to the payee's bank.
     */
    @Retryable(
            retryFor = BankTimeoutException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public BankResponse credit(String bankCode, String accountNumber,
                                Long amountPaisa, String txnId) {
        BankAdapter adapter = resolveAdapter(bankCode);
        log.info("Sending CREDIT to bank {} for txn {}", bankCode, txnId);

        try {
            return adapter.credit(accountNumber, amountPaisa, txnId);
        } catch (Exception e) {
            log.warn("Bank timeout during CREDIT for txn {}: {}", txnId, e.getMessage());
            throw new BankTimeoutException("Credit timeout for bank " + bankCode, e);
        }
    }

    /**
     * Send a reversal request to the payer's bank.
     */
    @Retryable(
            retryFor = BankTimeoutException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public BankResponse reversal(String bankCode, String originalTxnId) {
        BankAdapter adapter = resolveAdapter(bankCode);
        log.info("Sending REVERSAL for txn {} to bank {}", originalTxnId, bankCode);

        try {
            return adapter.reversal(originalTxnId);
        } catch (Exception e) {
            log.warn("Bank timeout during REVERSAL for txn {}: {}", originalTxnId, e.getMessage());
            throw new BankTimeoutException("Reversal timeout for bank " + bankCode, e);
        }
    }

    private BankAdapter resolveAdapter(String bankCode) {
        BankAdapter adapter = adapters.get(bankCode);
        if (adapter != null) {
            return adapter;
        }
        if (defaultAdapter != null) {
            return defaultAdapter;  // Route through NCHL switch
        }
        throw new BankNotSupportedException("Bank not connected: " + bankCode);
    }
}
