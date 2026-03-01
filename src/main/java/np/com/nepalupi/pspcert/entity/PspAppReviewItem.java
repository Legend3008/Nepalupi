package np.com.nepalupi.pspcert.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.pspcert.enums.ReviewResult;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "psp_app_review_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PspAppReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "certification_id", nullable = false)
    private UUID certificationId;

    @Column(name = "review_stage", nullable = false)
    private String reviewStage;

    @Column(name = "checklist_item", nullable = false)
    private String checklistItem;

    @Builder.Default
    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "result", nullable = false)
    private ReviewResult result = ReviewResult.PENDING;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
