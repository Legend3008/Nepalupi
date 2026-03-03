package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "grievances")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    @Builder.Default
    private String status = "OPEN";

    @Column(length = 20)
    @Builder.Default
    private String priority = "MEDIUM";

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "ticket_number", unique = true, length = 30)
    private String ticketNumber;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
