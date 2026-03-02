package np.com.nepalupi.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.operations.entity.Incident;
import np.com.nepalupi.operations.repository.IncidentRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monitoring Dashboard Service — aggregates real system health metrics.
 * <p>
 * Collects from actual DB state:
 * - Active incidents by severity
 * - Transaction success rate (last 24h)
 * - Average latency (from completed transactions)
 * - Total transaction volume
 * - Per-status transaction counts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringDashboardService {

    private final IncidentRepository incidentRepository;
    private final TransactionRepository transactionRepository;

    // Cached metrics (refreshed every 30s by health check)
    private final AtomicReference<Double> cachedSuccessRate = new AtomicReference<>(100.0);
    private final AtomicReference<Double> cachedAvgLatencyMs = new AtomicReference<>(0.0);
    private final AtomicLong cachedTotalTxns = new AtomicLong(0);

    /**
     * Get current system health snapshot with real metrics.
     */
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();

        // Active incidents
        var activeIncidents = incidentRepository.findActiveIncidents();
        health.put("activeIncidentCount", activeIncidents.size());
        health.put("activeIncidents", activeIncidents.stream()
                .map(i -> Map.of(
                        "number", i.getIncidentNumber(),
                        "severity", i.getSeverity(),
                        "title", i.getTitle(),
                        "status", i.getStatus().name(),
                        "engineer", i.getOnCallEngineer() != null ? i.getOnCallEngineer() : "UNASSIGNED"
                ))
                .toList());

        // Highest severity active incident
        int highestSev = activeIncidents.stream()
                .mapToInt(Incident::getSeverity)
                .min()
                .orElse(0);
        health.put("highestActiveSeverity", highestSev);

        // Overall status based on incidents
        String overallStatus;
        if (activeIncidents.isEmpty()) {
            overallStatus = "HEALTHY";
        } else if (highestSev <= 2) {
            overallStatus = "DEGRADED";
        } else {
            overallStatus = "WARNING";
        }
        health.put("overallStatus", overallStatus);

        // Real transaction metrics
        health.put("transactionSuccessRate", cachedSuccessRate.get());
        health.put("avgLatencyMs", cachedAvgLatencyMs.get());
        health.put("totalTransactions24h", cachedTotalTxns.get());

        // Transaction status breakdown (last 24h)
        Instant dayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        var allTxns = transactionRepository.findAll();
        Map<String, Long> statusBreakdown = new HashMap<>();
        for (TransactionStatus status : TransactionStatus.values()) {
            long count = allTxns.stream()
                    .filter(t -> t.getStatus() == status
                            && t.getCreatedAt() != null
                            && t.getCreatedAt().isAfter(dayAgo))
                    .count();
            if (count > 0) {
                statusBreakdown.put(status.name(), count);
            }
        }
        health.put("statusBreakdown", statusBreakdown);

        // Total volume (all time)
        long totalVolume = allTxns.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED && t.getAmount() != null)
                .mapToLong(t -> t.getAmount())
                .sum();
        health.put("totalVolumePaisa", totalVolume);
        health.put("totalVolumeNPR", totalVolume / 100.0);

        health.put("lastUpdateAt", Instant.now().toString());

        return health;
    }

    /**
     * Healthcheck — runs every 30 seconds to refresh cached metrics from DB.
     */
    @Scheduled(fixedRate = 30000)
    public void periodicHealthCheck() {
        try {
            Instant dayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
            var recentTxns = transactionRepository.findAll().stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(dayAgo))
                    .toList();

            long total = recentTxns.size();
            cachedTotalTxns.set(total);

            if (total > 0) {
                long completed = recentTxns.stream()
                        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                        .count();
                cachedSuccessRate.set(Math.round(completed * 10000.0 / total) / 100.0);

                double avgLatency = recentTxns.stream()
                        .filter(t -> t.getStatus() == TransactionStatus.COMPLETED
                                && t.getInitiatedAt() != null && t.getCompletedAt() != null)
                        .mapToLong(t -> java.time.Duration.between(t.getInitiatedAt(), t.getCompletedAt()).toMillis())
                        .average()
                        .orElse(0.0);
                cachedAvgLatencyMs.set(Math.round(avgLatency * 100.0) / 100.0);
            }

            log.trace("Health metrics refreshed: txns={}, successRate={}%, avgLatency={}ms",
                    total, cachedSuccessRate.get(), cachedAvgLatencyMs.get());
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
        }
    }
}
