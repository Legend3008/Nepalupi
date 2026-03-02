package np.com.nepalupi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Redis health indicator — verifies VPA cache connectivity.
 */
@Component("redisVpaCache")
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory connectionFactory;

    @Override
    public Health health() {
        try {
            var connection = connectionFactory.getConnection();
            String pong = new String(connection.commands().ping());
            connection.close();

            return Health.up()
                    .withDetail("status", "CONNECTED")
                    .withDetail("response", pong)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
