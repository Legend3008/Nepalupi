package np.com.nepalupi.operations.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "postmortem")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Postmortem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String title;

    @Column(name = "timeline_summary", columnDefinition = "TEXT")
    private String timelineSummary;

    @Column(name = "root_cause_analysis", nullable = false, columnDefinition = "TEXT")
    private String rootCauseAnalysis;

    @Column(name = "contributing_factors", columnDefinition = "TEXT")
    private String contributingFactors;

    @Column(name = "what_went_well", columnDefinition = "TEXT")
    private String whatWentWell;

    @Column(name = "what_went_poorly", columnDefinition = "TEXT")
    private String whatWentPoorly;

    @Column(name = "action_items", columnDefinition = "jsonb")
    private String actionItems;  // JSON array

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(columnDefinition = "jsonb")
    private String attendees;  // JSON array

    @Builder.Default
    private String status = "DRAFT";  // DRAFT / REVIEWED / COMPLETED

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
