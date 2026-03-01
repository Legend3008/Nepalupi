package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Sanctions list screening record — UN Security Council, INTERPOL, Nepal domestic lists.
 */
@Entity
@Table(name = "sanctions_screening")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SanctionsScreening {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** UN_SECURITY_COUNCIL / INTERPOL / NEPAL_DOMESTIC */
    @Column(name = "screened_against", nullable = false, length = 50)
    private String screenedAgainst;

    @Column(name = "match_found")
    @Builder.Default
    private Boolean matchFound = false;

    @Column(name = "match_details", columnDefinition = "TEXT")
    private String matchDetails;

    @CreationTimestamp
    @Column(name = "screened_at", updatable = false)
    private Instant screenedAt;
}
