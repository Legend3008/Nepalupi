package np.com.nepalupi.mandate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mandate_notification")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MandateNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mandate_id", nullable = false)
    private UUID mandateId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "notification_type", nullable = false)
    private String notificationType; // PRE_DEBIT, EXECUTION, EXPIRY

    @Column(name = "sent_at")
    @Builder.Default
    private Instant sentAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private String channel = "PUSH"; // PUSH, SMS, EMAIL

    @Column(nullable = false)
    @Builder.Default
    private String status = "SENT";

    @Column(columnDefinition = "TEXT")
    private String message;
}
