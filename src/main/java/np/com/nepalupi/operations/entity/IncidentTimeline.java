package np.com.nepalupi.operations.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.operations.enums.TimelineEntryType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_timeline")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private TimelineEntryType entryType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String author;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
