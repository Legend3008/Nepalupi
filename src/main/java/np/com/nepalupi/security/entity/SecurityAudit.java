package np.com.nepalupi.security.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.security.enums.AuditStatus;
import np.com.nepalupi.security.enums.AuditType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_audit")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SecurityAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false)
    private AuditType auditType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "audit_status", nullable = false)
    private AuditStatus auditStatus = AuditStatus.SCHEDULED;

    @Column(name = "auditor_name")
    private String auditorName;

    @Column(name = "auditor_firm")
    private String auditorFirm;

    @Column(name = "scope_description", columnDefinition = "TEXT")
    private String scopeDescription;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "next_audit_due")
    private Instant nextAuditDue;

    @Builder.Default
    @Column(name = "total_findings")
    private Integer totalFindings = 0;

    @Builder.Default
    @Column(name = "critical_findings")
    private Integer criticalFindings = 0;

    @Builder.Default
    @Column(name = "high_findings")
    private Integer highFindings = 0;

    @Builder.Default
    @Column(name = "medium_findings")
    private Integer mediumFindings = 0;

    @Builder.Default
    @Column(name = "low_findings")
    private Integer lowFindings = 0;

    @Column(name = "report_url")
    private String reportUrl;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Builder.Default
    @Column(name = "nrb_submitted")
    private Boolean nrbSubmitted = false;

    @Column(name = "nrb_submitted_at")
    private Instant nrbSubmittedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
