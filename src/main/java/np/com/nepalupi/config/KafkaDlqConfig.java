package np.com.nepalupi.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Section 16.4: Dead Letter Queue configuration for Kafka consumers.
 * <p>
 * Failed async operations → DLQ with exponential backoff (1s, 2s, 4s, 8s, max 5 retries).
 * Alert operations team if DLQ depth exceeds threshold.
 */
@Configuration
@Slf4j
public class KafkaDlqConfig {

    /**
     * Error handler that retries with exponential backoff,
     * then publishes to .DLT (Dead Letter Topic) on exhaustion.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // DLQ recoverer — publishes failed records to {topic}.DLT
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> {
                    log.error("Sending to DLQ: topic={}, key={}, error={}",
                            record.topic(), record.key(), ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(
                            record.topic() + ".DLT", record.partition());
                });

        // Exponential backoff: 1s initial, 2x multiplier, max 16s, 5 retries
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(60000L); // Max 60s total
        backOff.setMaxAttempts(5);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Don't retry on non-transient errors
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                com.fasterxml.jackson.core.JsonParseException.class
        );

        log.info("Kafka DLQ error handler configured: 5 retries with exponential backoff (1s→16s)");
        return errorHandler;
    }
}
