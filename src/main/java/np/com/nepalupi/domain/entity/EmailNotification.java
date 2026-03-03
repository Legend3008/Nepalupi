package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "to_email", nullable = false, length = 255)
    private String toEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "template_name", length = 50)
    private String templateName;

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
