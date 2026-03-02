package np.com.nepalupi.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Micrometer metrics for UPI transaction observability.
 * <p>
 * Exposes metrics to Prometheus via /actuator/prometheus:
 * - upi_transactions_total (counter): Total transactions by status
 * - upi_transaction_duration (timer): Payment processing latency
 * - upi_fraud_flags_total (counter): Fraud flags raised
 * - upi_vpa_resolutions_total (counter): VPA lookups
 * - upi_bank_calls_total (counter): Bank adapter calls by type and result
 */
@Component
@Getter
public class TransactionMetrics {

    // ── Transaction counters ──
    private final Counter txnInitiated;
    private final Counter txnCompleted;
    private final Counter txnFailed;
    private final Counter txnRefund;

    // ── Timing ──
    private final Timer txnDuration;

    // ── Fraud ──
    private final Counter fraudFlags;

    // ── VPA ──
    private final Counter vpaResolutions;
    private final Counter vpaCacheHits;
    private final Counter vpaCacheMisses;

    // ── Bank ──
    private final Counter bankDebitSuccess;
    private final Counter bankDebitFailed;
    private final Counter bankCreditSuccess;
    private final Counter bankCreditFailed;

    // ── Collect/Mandate ──
    private final Counter collectRequests;
    private final Counter mandateExecutions;

    public TransactionMetrics(MeterRegistry registry) {
        // Transaction lifecycle
        this.txnInitiated = Counter.builder("upi_transactions_total")
                .tag("status", "initiated")
                .description("Total transactions initiated")
                .register(registry);

        this.txnCompleted = Counter.builder("upi_transactions_total")
                .tag("status", "completed")
                .description("Total transactions completed successfully")
                .register(registry);

        this.txnFailed = Counter.builder("upi_transactions_total")
                .tag("status", "failed")
                .description("Total transactions failed")
                .register(registry);

        this.txnRefund = Counter.builder("upi_transactions_total")
                .tag("status", "refund")
                .description("Total refund transactions")
                .register(registry);

        // Processing time
        this.txnDuration = Timer.builder("upi_transaction_duration")
                .description("Transaction processing duration")
                .register(registry);

        // Fraud engine
        this.fraudFlags = Counter.builder("upi_fraud_flags_total")
                .description("Total fraud flags raised")
                .register(registry);

        // VPA resolution
        this.vpaResolutions = Counter.builder("upi_vpa_resolutions_total")
                .description("Total VPA resolution requests")
                .register(registry);

        this.vpaCacheHits = Counter.builder("upi_vpa_cache_total")
                .tag("result", "hit")
                .description("VPA cache hits")
                .register(registry);

        this.vpaCacheMisses = Counter.builder("upi_vpa_cache_total")
                .tag("result", "miss")
                .description("VPA cache misses")
                .register(registry);

        // Bank adapter calls
        this.bankDebitSuccess = Counter.builder("upi_bank_calls_total")
                .tag("type", "debit").tag("result", "success")
                .register(registry);

        this.bankDebitFailed = Counter.builder("upi_bank_calls_total")
                .tag("type", "debit").tag("result", "failed")
                .register(registry);

        this.bankCreditSuccess = Counter.builder("upi_bank_calls_total")
                .tag("type", "credit").tag("result", "success")
                .register(registry);

        this.bankCreditFailed = Counter.builder("upi_bank_calls_total")
                .tag("type", "credit").tag("result", "failed")
                .register(registry);

        // Collect / Mandate
        this.collectRequests = Counter.builder("upi_collect_requests_total")
                .description("Total collect requests created")
                .register(registry);

        this.mandateExecutions = Counter.builder("upi_mandate_executions_total")
                .description("Total mandate executions attempted")
                .register(registry);
    }
}
