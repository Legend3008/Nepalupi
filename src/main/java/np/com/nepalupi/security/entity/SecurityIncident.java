package np.com.nepalupi.security.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.security.enums.SecurityIncidentStatus;
import np.com.nepalupi.security.enums.SecurityIncidentType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_incident")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SecurityIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_reference", nullable = false, unique = true)
    private String incidentReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false)
    private SecurityIncidentType incidentType;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "attack_vector")
    private String attackVector;

    @Column(name = "affected_systems", columnDefinition = "TEXT")
    private String affectedSystems;

    @Builder.Default
    @Column(name = "data_compromised")
    private Boolean dataCompromised = false;

    @Column(name = "data_compromised_description", columnDefinition = "TEXT")
    private String dataCompromisedDescription;

    @Builder.Default
    @Column(name = "users_affected_count")
    private Integer usersAffectedCount = 0;

    @Builder.Default
    @Column(name = "financial_impact_paisa")
    private Long financialImpactPaisa = 0L;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private SecurityIncidentStatus status = SecurityIncidentStatus.DETECTED;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "contained_at")
    private Instant containedAt;

    @Column(name = "eradicated_at")
    private Instant eradicatedAt;

    @Column(name = "recovered_at")
    private Instant recoveredAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Builder.Default
    @Column(name = "nrb_notified")
    private Boolean nrbNotified = false;

    @Column(name = "nrb_notified_at")
    private Instant nrbNotifiedAt;

    @Builder.Default
    @Column(name = "users_notified")
    private Boolean usersNotified = false;

    @Column(name = "users_notified_at")
    private Instant usersNotifiedAt;

    @Builder.Default
    @Column(name = "law_enforcement_notified")
    private Boolean lawEnforcementNotified = false;

    @Column(name = "incident_commander")
    private String incidentCommander;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
