package np.com.nepalupi.operations.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.operations.enums.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_number", unique = true, nullable = false)
    private String incidentNumber;

    @Column(nullable = false)
    private Integer severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status = IncidentStatus.DETECTED;

    // Timing
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    // Staff
    @Column(name = "on_call_engineer")
    private String onCallEngineer;

    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    @Column(name = "escalated_to")
    private String escalatedTo;

    // Communication
    @Column(name = "slack_channel")
    private String slackChannel;

    @Column(name = "nrb_notified")
    @Builder.Default
    private Boolean nrbNotified = false;

    @Column(name = "nrb_notified_at")
    private Instant nrbNotifiedAt;

    @Column(name = "psp_notified")
    @Builder.Default
    private Boolean pspNotified = false;

    @Column(name = "status_page_updated")
    @Builder.Default
    private Boolean statusPageUpdated = false;

    // Resolution
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    // Metadata
    @Column(name = "affected_service")
    private String affectedService;

    @Column(name = "affected_bank_code")
    private String affectedBankCode;

    @Column(name = "impact_description", columnDefinition = "TEXT")
    private String impactDescription;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (detectedAt == null) detectedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
