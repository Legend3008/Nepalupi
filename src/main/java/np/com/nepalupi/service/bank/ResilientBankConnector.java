package np.com.nepalupi.service.bank;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
import java.util.function.Supplier;

/**
 * Central connector that routes bank operations to the correct {@link BankAdapter}.
 * <p>
 * Section 16.3: Circuit breaker pattern per bank.
 * If bank failure rate > 50% in 60s → circuit OPEN → reject new requests.
 * Half-open after 30s → allow probe requests.
 * Close circuit when success rate recovers.
 * <p>
 * Includes retry with exponential backoff for transient bank timeouts (Section 16.1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientBankConnector {

    private final List<BankAdapter> adapterList;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
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
        log.info("ResilientBankConnector: registered {} bank adapters with circuit breakers", adapters.size());
    }

    /**
     * Debit with circuit breaker + retry.
     */
    @Retryable(retryFor = BankTimeoutException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public BankResponse debit(String bankCode, String accountNumber,
                               Long amountPaisa, String txnId) {
        return executeWithCircuitBreaker(bankCode, () -> {
            BankAdapter adapter = resolveAdapter(bankCode);
            log.info("DEBIT via circuit breaker: bank={}, txn={}", bankCode, txnId);
            return adapter.debit(accountNumber, amountPaisa, txnId);
        }, "DEBIT", txnId);
    }

    /**
     * Credit with circuit breaker + retry.
     */
    @Retryable(retryFor = BankTimeoutException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public BankResponse credit(String bankCode, String accountNumber,
                                Long amountPaisa, String txnId) {
        return executeWithCircuitBreaker(bankCode, () -> {
            BankAdapter adapter = resolveAdapter(bankCode);
            log.info("CREDIT via circuit breaker: bank={}, txn={}", bankCode, txnId);
            return adapter.credit(accountNumber, amountPaisa, txnId);
        }, "CREDIT", txnId);
    }

    /**
     * Reversal with circuit breaker + retry.
     */
    @Retryable(retryFor = BankTimeoutException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public BankResponse reversal(String bankCode, String originalTxnId) {
        return executeWithCircuitBreaker(bankCode, () -> {
            BankAdapter adapter = resolveAdapter(bankCode);
            log.info("REVERSAL via circuit breaker: bank={}, txn={}", bankCode, originalTxnId);
            return adapter.reversal(originalTxnId);
        }, "REVERSAL", originalTxnId);
    }

    /**
     * Balance check with circuit breaker (fewer retries).
     */
    @Retryable(retryFor = BankTimeoutException.class, maxAttempts = 2,
            backoff = @Backoff(delay = 300, multiplier = 2))
    public BankResponse checkBalance(String bankCode, String accountNumber) {
        return executeWithCircuitBreaker(bankCode, () -> {
            BankAdapter adapter = resolveAdapter(bankCode);
            log.info("BALANCE CHECK via circuit breaker: bank={}", bankCode);
            return adapter.checkBalance(accountNumber);
        }, "BALANCE_CHECK", accountNumber);
    }

    /**
     * Get circuit breaker state for a bank (for monitoring dashboard).
     */
    public String getCircuitBreakerState(String bankCode) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("bank-" + bankCode);
        return cb.getState().name();
    }

    /**
     * Get circuit breaker metrics for all banks.
     */
    public Map<String, Map<String, Object>> getAllCircuitBreakerMetrics() {
        Map<String, Map<String, Object>> metrics = new HashMap<>();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbMetrics = new HashMap<>();
            cbMetrics.put("state", cb.getState().name());
            cbMetrics.put("failureRate", cb.getMetrics().getFailureRate());
            cbMetrics.put("slowCallRate", cb.getMetrics().getSlowCallRate());
            cbMetrics.put("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            cbMetrics.put("failedCalls", cb.getMetrics().getNumberOfFailedCalls());
            cbMetrics.put("successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbMetrics.put("notPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls());
            metrics.put(cb.getName(), cbMetrics);
        });
        return metrics;
    }

    private BankResponse executeWithCircuitBreaker(String bankCode, Supplier<BankResponse> operation,
                                                     String operationType, String reference) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("bank-" + bankCode);
        try {
            return CircuitBreaker.decorateSupplier(cb, operation).get();
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            log.error("Circuit OPEN for bank {}. Rejecting {} for ref={}", bankCode, operationType, reference);
            throw new BankTimeoutException("Bank " + bankCode + " circuit breaker is OPEN — service unavailable", e);
        } catch (Exception e) {
            log.warn("Bank call failed through circuit breaker: bank={}, op={}, ref={}, error={}",
                    bankCode, operationType, reference, e.getMessage());
            throw new BankTimeoutException(operationType + " failed for bank " + bankCode, e);
        }
    }

    private BankAdapter resolveAdapter(String bankCode) {
        BankAdapter adapter = adapters.get(bankCode);
        if (adapter != null) return adapter;
        if (defaultAdapter != null) return defaultAdapter;
        throw new BankNotSupportedException("Bank not connected: " + bankCode);
    }
}
