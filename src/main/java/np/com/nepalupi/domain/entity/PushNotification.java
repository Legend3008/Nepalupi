package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PushNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "data_payload", columnDefinition = "jsonb")
    private String dataPayload;

    @Column(length = 20)
    @Builder.Default
    private String status = "QUEUED";

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
