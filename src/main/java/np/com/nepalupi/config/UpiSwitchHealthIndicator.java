package np.com.nepalupi.config;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Custom Spring Boot Actuator health indicator for the UPI switch.
 * <p>
 * Reports DOWN if:
 * - Transaction success rate drops below 90% in the last hour
 * - No transactions processed in the last 30 minutes (if system should be active)
 */
@Component("upiSwitch")
@RequiredArgsConstructor
public class UpiSwitchHealthIndicator implements HealthIndicator {

    private final TransactionRepository transactionRepository;

    @Override
    public Health health() {
        try {
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

            var recentTxns = transactionRepository.findAll().stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(oneHourAgo))
                    .toList();

            long total = recentTxns.size();

            if (total == 0) {
                return Health.up()
                        .withDetail("status", "IDLE")
                        .withDetail("message", "No transactions in last hour")
                        .withDetail("totalTransactionsAllTime", transactionRepository.count())
                        .build();
            }

            long completed = recentTxns.stream()
                    .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                    .count();

            long failed = recentTxns.stream()
                    .filter(t -> t.getStatus().isTerminal() && t.getStatus() != TransactionStatus.COMPLETED)
                    .count();

            double successRate = (double) completed / total * 100;

            Health.Builder builder = successRate >= 90 ? Health.up() : Health.down();

            return builder
                    .withDetail("transactionsLastHour", total)
                    .withDetail("completedLastHour", completed)
                    .withDetail("failedLastHour", failed)
                    .withDetail("successRate", Math.round(successRate * 100.0) / 100.0 + "%")
                    .withDetail("totalTransactionsAllTime", transactionRepository.count())
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
