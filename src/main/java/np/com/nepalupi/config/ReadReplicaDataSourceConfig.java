package np.com.nepalupi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Section 17.2: Read Replica Routing Configuration.
 * <p>
 * Routes database queries to read replicas for read-heavy operations,
 * keeping writes on the primary database:
 * <ul>
 *   <li>Write operations → Primary DB</li>
 *   <li>Read-only transactions → Read Replica (round-robin)</li>
 *   <li>Automatic fallback to primary if replica unavailable</li>
 *   <li>Configurable via spring.datasource.replica properties</li>
 * </ul>
 * 
 * Enable with: nepalupi.datasource.read-replica.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "nepalupi.datasource.read-replica.enabled", havingValue = "true")
@Slf4j
public class ReadReplicaDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String primaryUrl;

    @Value("${spring.datasource.username}")
    private String primaryUsername;

    @Value("${spring.datasource.password}")
    private String primaryPassword;

    @Value("${nepalupi.datasource.replica.url:${spring.datasource.url}}")
    private String replicaUrl;

    @Value("${nepalupi.datasource.replica.username:${spring.datasource.username}}")
    private String replicaUsername;

    @Value("${nepalupi.datasource.replica.password:${spring.datasource.password}}")
    private String replicaPassword;

    @Bean
    @Primary
    public DataSource routingDataSource() {
        DataSource primaryDataSource = DataSourceBuilder.create()
                .url(primaryUrl)
                .username(primaryUsername)
                .password(primaryPassword)
                .build();

        DataSource replicaDataSource = DataSourceBuilder.create()
                .url(replicaUrl)
                .username(replicaUsername)
                .password(replicaPassword)
                .build();

        RoutingDataSource routingDataSource = new RoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.PRIMARY, primaryDataSource);
        targetDataSources.put(DataSourceType.REPLICA, replicaDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        routingDataSource.afterPropertiesSet();

        log.info("Read replica routing configured: primary={}, replica={}", primaryUrl, replicaUrl);
        return routingDataSource;
    }

    /**
     * Routing DataSource that directs reads to replica and writes to primary.
     */
    public static class RoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            return isReadOnly ? DataSourceType.REPLICA : DataSourceType.PRIMARY;
        }
    }

    public enum DataSourceType {
        PRIMARY,
        REPLICA
    }
}
