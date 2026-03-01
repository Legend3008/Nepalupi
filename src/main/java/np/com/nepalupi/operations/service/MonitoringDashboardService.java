package np.com.nepalupi.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.operations.entity.Incident;
import np.com.nepalupi.operations.repository.IncidentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitoring Dashboard Service — aggregates system health metrics.
 * <p>
 * Provides real-time system health overview for the operations dashboard:
 * - Active incidents by severity
 * - Transaction success rate
 * - Average latency
 * - Bank connectivity status
 * - Kafka consumer lag
 * - Database connection pool usage
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringDashboardService {

    private final IncidentRepository incidentRepository;

    /**
     * Get current system health snapshot.
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

        // Overall status
        String overallStatus;
        if (activeIncidents.isEmpty()) {
            overallStatus = "HEALTHY";
        } else if (highestSev <= 2) {
            overallStatus = "DEGRADED";
        } else {
            overallStatus = "WARNING";
        }
        health.put("overallStatus", overallStatus);

        // Simulated metrics (in production: pull from Prometheus / Grafana)
        health.put("transactionSuccessRate", 99.7);
        health.put("avgLatencyMs", 180);
        health.put("kafkaConsumerLag", 12);
        health.put("dbConnectionPoolUsage", 45);
        health.put("lastUpdateAt", Instant.now().toString());

        return health;
    }

    /**
     * Healthcheck — runs every 30 seconds.
     * In production, auto-creates incidents when thresholds are breached.
     */
    @Scheduled(fixedRate = 30000)
    public void periodicHealthCheck() {
        // In production:
        // - Check transaction success rate < 95% → auto-create SEV2
        // - Check avg latency > 5000ms → auto-create SEV3
        // - Check Kafka lag > 10000 → auto-create SEV3
        // - Check any bank adapter down → auto-create SEV3
        log.trace("Periodic health check completed");
    }
}
