package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Suspicious Transaction Report (STR) — filed with FIU when AML patterns detected.
 */
@Entity
@Table(name = "suspicious_transaction_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SuspiciousTransactionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "user_id")
    private UUID userId;

    /** STRUCTURING / LAYERING / VELOCITY / CIRCULAR / SANCTIONS_HIT */
    @Column(name = "suspicion_type", nullable = false, length = 50)
    private String suspicionType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** JSON signals that triggered the alert */
    @Column(columnDefinition = "jsonb", nullable = false)
    private String signals;

    @Column(name = "compliance_officer", length = 200)
    private String complianceOfficer;

    /** PENDING_REVIEW / CLEARED / FILED / ESCALATED */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_REVIEW";

    @Column(name = "filed_with_fiu")
    @Builder.Default
    private Boolean filedWithFiu = false;

    @Column(name = "fiu_reference", length = 100)
    private String fiuReference;

    @Column(name = "filed_at")
    private Instant filedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
