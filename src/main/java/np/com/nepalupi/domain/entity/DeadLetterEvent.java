package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 16.4: Dead letter event tracking.
 * Persists failed Kafka events for monitoring, retry, and ops alerting.
 */
@Entity
@Table(name = "dead_letter_event")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeadLetterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key")
    private String eventKey;

    @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
    private String eventPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 5;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, RETRYING, RESOLVED, ABANDONED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
