package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Immutable, append-only compliance audit log.
 * Every significant system event is recorded here for NRB audit trail.
 */
@Entity
@Table(name = "compliance_audit_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ComplianceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** TXN_CREATED / TXN_STATE_CHANGE / DISPUTE_RAISED / STR_FILED / PSP_ONBOARDED / SETTLEMENT_GENERATED */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** TRANSACTION / DISPUTE / PSP / SETTLEMENT / USER / VPA */
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    /** JSON details of the event */
    @Column(columnDefinition = "jsonb", nullable = false)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
