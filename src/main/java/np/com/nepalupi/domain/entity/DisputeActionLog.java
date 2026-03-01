package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log for every action taken on a dispute.
 * <p>
 * NRB can ask to see full dispute trails at any time.
 * Every action, every communication, every decision is recorded here.
 */
@Entity
@Table(name = "dispute_action_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DisputeActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dispute_id", nullable = false)
    private UUID disputeId;

    /** RAISED / ACKNOWLEDGED / BANK_QUERY_SENT / BANK_RESPONSE / AUTO_RESOLVED / MANUAL_ESCALATED / RESOLVED / CLOSED */
    @Column(nullable = false, length = 50)
    private String action;

    /** "system" for automated, or ops agent name for manual actions */
    @Column(name = "performed_by", length = 200)
    private String performedBy;

    /** Action details / notes */
    @Column(columnDefinition = "TEXT")
    private String details;

    /** Optional attachment (evidence doc URL) */
    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
