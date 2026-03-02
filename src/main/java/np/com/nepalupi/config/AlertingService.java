package np.com.nepalupi.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.repository.DeadLetterEventRepository;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.service.bank.ResilientBankConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Section 18.4: Alerting and SLO/SLI monitoring.
 * <p>
 * Alert rules:
 * - Success rate < 95% → P1 alert
 * - P99 latency > 5s → P2 alert
 * - Bank circuit breaker OPEN → P1 alert
 * - Settlement mismatch → P1 alert
 * - DLQ depth > 1000 → P2 alert
 * <p>
 * SLOs:
 * - Transaction success rate: ≥ 99.5%
 * - End-to-end latency p99: < 5 seconds
 * - System uptime: ≥ 99.99%
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertingService {

    private final MeterRegistry meterRegistry;
    private final DeadLetterEventRepository dlqRepository;
    private final ResilientBankConnector bankConnector;

    private final AtomicLong dlqDepth = new AtomicLong(0);
    private final AtomicLong openCircuitBreakers = new AtomicLong(0);

    @PostConstruct
    void init() {
        // Register DLQ depth gauge
        Gauge.builder("upi_dlq_depth", dlqDepth, AtomicLong::get)
                .description("Number of pending dead letter events")
                .tag("application", "nepal-upi-switch")
                .register(meterRegistry);

        // Register open circuit breaker count
        Gauge.builder("upi_circuit_breakers_open", openCircuitBreakers, AtomicLong::get)
                .description("Number of bank circuit breakers in OPEN state")
                .tag("application", "nepal-upi-switch")
                .register(meterRegistry);

        log.info("Alerting service initialized with DLQ and circuit breaker monitoring");
    }

    /**
     * Check alert conditions every 30 seconds.
     */
    @Scheduled(fixedRate = 30_000)
    public void checkAlerts() {
        checkDlqDepth();
        checkCircuitBreakers();
    }

    /**
     * Check DLQ depth — P2 alert if > 1000.
     */
    private void checkDlqDepth() {
        try {
            long depth = dlqRepository.countByStatus("PENDING");
            dlqDepth.set(depth);

            if (depth > 1000) {
                log.error("P2 ALERT: DLQ depth exceeds threshold! depth={}, threshold=1000", depth);
                fireAlert("P2", "DLQ_DEPTH_HIGH",
                        "Dead letter queue depth is " + depth + " (threshold: 1000)");
            } else if (depth > 100) {
                log.warn("DLQ depth is elevated: depth={}", depth);
            }
        } catch (Exception e) {
            log.warn("Could not check DLQ depth: {}", e.getMessage());
        }
    }

    /**
     * Check circuit breaker states — P1 alert if any bank is OPEN.
     */
    private void checkCircuitBreakers() {
        try {
            Map<String, Map<String, Object>> metrics = bankConnector.getAllCircuitBreakerMetrics();
            long openCount = metrics.values().stream()
                    .filter(m -> "OPEN".equals(m.get("state")))
                    .count();

            openCircuitBreakers.set(openCount);

            if (openCount > 0) {
                String openBanks = metrics.entrySet().stream()
                        .filter(e -> "OPEN".equals(e.getValue().get("state")))
                        .map(Map.Entry::getKey)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                log.error("P1 ALERT: Bank circuit breaker(s) OPEN! banks=[{}]", openBanks);
                fireAlert("P1", "CIRCUIT_BREAKER_OPEN",
                        "Bank circuit breakers are OPEN: " + openBanks);
            }
        } catch (Exception e) {
            log.warn("Could not check circuit breakers: {}", e.getMessage());
        }
    }

    /**
     * Fire an alert — in production, this would integrate with PagerDuty/Opsgenie/Slack.
     */
    private void fireAlert(String severity, String alertType, String message) {
        // In production: call PagerDuty/Opsgenie API
        // For now: log as structured alert
        log.error("╔══ ALERT ═══════════════════════════════════╗");
        log.error("║ Severity: {} | Type: {}", severity, alertType);
        log.error("║ Message: {}", message);
        log.error("╚════════════════════════════════════════════╝");
    }
}
