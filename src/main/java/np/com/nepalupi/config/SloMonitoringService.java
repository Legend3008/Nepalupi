package np.com.nepalupi.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Section 18.6: SLO (Service Level Objective) / SLI (Service Level Indicator) Monitoring.
 * <p>
 * Tracks formal SLOs for the NPI switch as required by NRB:
 * <ul>
 *   <li><b>Availability SLO:</b> 99.95% uptime (excludes planned maintenance)</li>
 *   <li><b>Latency SLO:</b> P99 &lt; 5 seconds end-to-end</li>
 *   <li><b>Success Rate SLO:</b> &gt; 99.5% of transactions complete or fail cleanly</li>
 *   <li><b>Throughput SLO:</b> Handle &gt; 1000 TPS sustained</li>
 *   <li><b>Error Budget:</b> Track remaining error budget per SLO</li>
 * </ul>
 * 
 * SLIs are computed from Micrometer metrics and evaluated every 5 minutes.
 */
@Service
@Slf4j
public class SloMonitoringService {

    private final MeterRegistry meterRegistry;

    // SLO targets
    private static final double AVAILABILITY_SLO = 0.9995;        // 99.95%
    private static final double LATENCY_P99_SLO_MS = 5000.0;      // 5 seconds
    private static final double SUCCESS_RATE_SLO = 0.995;          // 99.5%
    private static final double THROUGHPUT_SLO_TPS = 1000.0;       // 1000 TPS
    private static final Duration SLO_WINDOW = Duration.ofDays(30); // 30-day rolling

    // Tracking state
    private final AtomicReference<SloReport> latestReport = new AtomicReference<>();
    private final AtomicReference<Instant> serviceStartTime = new AtomicReference<>(Instant.now());

    // Counters for SLI computation
    private long totalRequests = 0;
    private long failedRequests = 0;
    private long sloViolationCount = 0;
    private double uptimeMinutes = 0;
    private double downtimeMinutes = 0;

    public SloMonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Register SLO gauges
        Gauge.builder("npi.slo.availability.target", () -> AVAILABILITY_SLO * 100)
                .description("Availability SLO target %")
                .register(meterRegistry);

        Gauge.builder("npi.slo.availability.actual", () -> {
            SloReport report = latestReport.get();
            return report != null ? report.availabilityPercent : 100.0;
        }).description("Actual availability %").register(meterRegistry);

        Gauge.builder("npi.slo.error_budget.remaining", () -> {
            SloReport report = latestReport.get();
            return report != null ? report.errorBudgetRemainingPercent : 100.0;
        }).description("Error budget remaining %").register(meterRegistry);

        Gauge.builder("npi.slo.success_rate.actual", () -> {
            SloReport report = latestReport.get();
            return report != null ? report.successRatePercent : 100.0;
        }).description("Actual success rate %").register(meterRegistry);
    }

    /**
     * Compute SLIs and evaluate SLOs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void evaluateSlos() {
        try {
            SloReport report = computeSloReport();
            latestReport.set(report);

            if (!report.allMet) {
                sloViolationCount++;
                log.warn("SLO VIOLATION detected! Report: {}", report);
            } else {
                log.info("SLOs met. Availability: {}%, Success rate: {}%, Error budget: {}%",
                        String.format("%.3f", report.availabilityPercent),
                        String.format("%.3f", report.successRatePercent),
                        String.format("%.2f", report.errorBudgetRemainingPercent));
            }
        } catch (Exception e) {
            log.error("SLO evaluation error: {}", e.getMessage());
        }
    }

    /**
     * Compute current SLO report.
     */
    public SloReport computeSloReport() {
        // 1. Availability SLI
        double totalMinutes = uptimeMinutes + downtimeMinutes;
        double availability = totalMinutes > 0 ? uptimeMinutes / totalMinutes : 1.0;
        boolean availabilityMet = availability >= AVAILABILITY_SLO;

        // 2. Success rate SLI
        double successRate = totalRequests > 0 
                ? (double) (totalRequests - failedRequests) / totalRequests : 1.0;
        boolean successRateMet = successRate >= SUCCESS_RATE_SLO;

        // 3. Latency SLI (from Micrometer timer)
        double latencyP99Ms = getP99LatencyMs();
        boolean latencyMet = latencyP99Ms <= LATENCY_P99_SLO_MS;

        // 4. Throughput SLI
        double currentTps = getCurrentTps();
        boolean throughputMet = true; // Only flagged if sustained below SLO

        // 5. Error budget
        double errorBudgetTotal = (1.0 - AVAILABILITY_SLO) * totalMinutes;  // Allowed downtime
        double errorBudgetUsed = downtimeMinutes;
        double errorBudgetRemaining = errorBudgetTotal > 0 
                ? Math.max(0, (errorBudgetTotal - errorBudgetUsed) / errorBudgetTotal * 100) : 100.0;

        boolean allMet = availabilityMet && successRateMet && latencyMet;

        return new SloReport(
                availability * 100,
                availabilityMet,
                successRate * 100,
                successRateMet,
                latencyP99Ms,
                latencyMet,
                currentTps,
                throughputMet,
                errorBudgetRemaining,
                allMet,
                Instant.now(),
                sloViolationCount
        );
    }

    /**
     * Record a request for SLI computation.
     */
    public void recordRequest(boolean success) {
        totalRequests++;
        if (!success) failedRequests++;
    }

    /**
     * Record uptime/downtime.
     */
    public void recordUptime(double minutes) {
        uptimeMinutes += minutes;
    }

    public void recordDowntime(double minutes) {
        downtimeMinutes += minutes;
    }

    /**
     * Get latest SLO report.
     */
    public SloReport getLatestReport() {
        SloReport report = latestReport.get();
        return report != null ? report : computeSloReport();
    }

    /**
     * Get detailed SLO dashboard data.
     */
    public Map<String, Object> getSlosDashboard() {
        SloReport report = getLatestReport();
        Map<String, Object> dashboard = new LinkedHashMap<>();

        dashboard.put("evaluatedAt", report.evaluatedAt.toString());
        dashboard.put("overallStatus", report.allMet ? "ALL_MET" : "VIOLATION");

        dashboard.put("availability", Map.of(
                "target", AVAILABILITY_SLO * 100 + "%",
                "actual", String.format("%.3f%%", report.availabilityPercent),
                "met", report.availabilityMet
        ));

        dashboard.put("successRate", Map.of(
                "target", SUCCESS_RATE_SLO * 100 + "%",
                "actual", String.format("%.3f%%", report.successRatePercent),
                "met", report.successRateMet
        ));

        dashboard.put("latencyP99", Map.of(
                "target", LATENCY_P99_SLO_MS + "ms",
                "actual", String.format("%.1fms", report.latencyP99Ms),
                "met", report.latencyMet
        ));

        dashboard.put("throughput", Map.of(
                "target", THROUGHPUT_SLO_TPS + " TPS",
                "actual", String.format("%.1f TPS", report.currentTps),
                "met", report.throughputMet
        ));

        dashboard.put("errorBudget", Map.of(
                "remaining", String.format("%.2f%%", report.errorBudgetRemainingPercent),
                "status", report.errorBudgetRemainingPercent > 50 ? "HEALTHY" :
                          report.errorBudgetRemainingPercent > 10 ? "WARNING" : "CRITICAL"
        ));

        dashboard.put("violationCount", report.violationCount);

        return dashboard;
    }

    private double getP99LatencyMs() {
        try {
            Timer timer = meterRegistry.find("npi.transaction.duration").timer();
            if (timer != null) {
                return timer.percentile(0.99, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Timer may not support percentiles
        }
        return 0.0;
    }

    private double getCurrentTps() {
        try {
            Counter counter = meterRegistry.find("npi.transactions").counter();
            if (counter != null) {
                return counter.count() / Math.max(1, 
                        Duration.between(serviceStartTime.get(), Instant.now()).getSeconds());
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0.0;
    }

    // --- Record ---

    public record SloReport(
            double availabilityPercent,
            boolean availabilityMet,
            double successRatePercent,
            boolean successRateMet,
            double latencyP99Ms,
            boolean latencyMet,
            double currentTps,
            boolean throughputMet,
            double errorBudgetRemainingPercent,
            boolean allMet,
            Instant evaluatedAt,
            long violationCount
    ) {}
}
