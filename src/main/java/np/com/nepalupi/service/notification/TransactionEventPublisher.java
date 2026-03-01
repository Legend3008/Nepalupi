package np.com.nepalupi.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.event.TransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes transaction events to Kafka for async processing
 * by notification, audit, analytics, and settlement consumers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventPublisher {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${nepalupi.kafka.topics.transaction-events:transaction-events}")
    private String transactionEventsTopic;

    /**
     * Publish a transaction event to Kafka.
     * Key = UPI txn ID (ensures ordering per transaction).
     */
    public void publish(TransactionEvent event) {
        log.info("Publishing event: txn={}, status={}", event.getUpiTxnId(), event.getStatus());

        kafkaTemplate.send(transactionEventsTopic, event.getUpiTxnId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event for txn {}: {}",
                                event.getUpiTxnId(), ex.getMessage());
                    } else {
                        log.debug("Event published for txn {} to partition {}",
                                event.getUpiTxnId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
