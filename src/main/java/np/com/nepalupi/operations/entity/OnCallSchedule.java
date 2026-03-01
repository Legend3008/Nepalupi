package np.com.nepalupi.operations.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "on_call_schedule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnCallSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "engineer_name", nullable = false)
    private String engineerName;

    @Column(name = "engineer_email", nullable = false)
    private String engineerEmail;

    @Column(name = "engineer_phone", nullable = false)
    private String engineerPhone;

    @Column(nullable = false)
    @Builder.Default
    private String role = "PRIMARY";  // PRIMARY / SECONDARY

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false)
    private LocalDate weekEnd;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
