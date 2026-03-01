package np.com.nepalupi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic auto-creation for development.
 * In production, topics should be pre-created with proper replication factor.
 */
@Configuration
public class KafkaConfig {

    @Value("${nepalupi.kafka.topics.transaction-events:transaction-events}")
    private String transactionEventsTopic;

    @Value("${nepalupi.kafka.topics.settlement-events:settlement-events}")
    private String settlementEventsTopic;

    @Value("${nepalupi.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    @Value("${nepalupi.kafka.topics.audit-events:audit-events}")
    private String auditEventsTopic;

    @Bean
    public NewTopic transactionEventsTopic() {
        return TopicBuilder.name(transactionEventsTopic)
                .partitions(6)
                .replicas(1)     // In production: 3
                .build();
    }

    @Bean
    public NewTopic settlementEventsTopic() {
        return TopicBuilder.name(settlementEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(notificationEventsTopic)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name(auditEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
