package np.com.nepalupi.mandate.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.mandate.enums.MandateExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "mandate_execution")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MandateExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mandate_id", nullable = false)
    private UUID mandateId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MandateExecutionStatus status = MandateExecutionStatus.SCHEDULED;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "pre_notification_sent_at")
    private Instant preNotificationSentAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
