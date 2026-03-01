package np.com.nepalupi.security.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.security.enums.FindingCategory;
import np.com.nepalupi.security.enums.FindingSeverity;
import np.com.nepalupi.security.enums.FindingStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_finding")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SecurityFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "audit_id", nullable = false)
    private UUID auditId;

    @Column(name = "finding_reference", nullable = false)
    private String findingReference;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private FindingSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private FindingCategory category;

    @Column(name = "affected_component")
    private String affectedComponent;

    @Column(name = "reproduction_steps", columnDefinition = "TEXT")
    private String reproductionSteps;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "recommended_fix", columnDefinition = "TEXT")
    private String recommendedFix;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private FindingStatus status = FindingStatus.OPEN;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "remediation_deadline")
    private Instant remediationDeadline;

    @Column(name = "fixed_at")
    private Instant fixedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
