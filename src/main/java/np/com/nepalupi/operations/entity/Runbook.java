package np.com.nepalupi.operations.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.operations.enums.RunbookCategory;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "runbook")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Runbook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunbookCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String symptoms;

    @Column(name = "diagnostic_steps", nullable = false, columnDefinition = "TEXT")
    private String diagnosticSteps;

    @Column(name = "remediation_steps", nullable = false, columnDefinition = "TEXT")
    private String remediationSteps;

    @Column(name = "verification_steps", nullable = false, columnDefinition = "TEXT")
    private String verificationSteps;

    @Column(name = "escalation_path", columnDefinition = "TEXT")
    private String escalationPath;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
