package np.com.nepalupi.mandate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "collect_spam_tracker")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CollectSpamTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requestor_vpa", nullable = false)
    private String requestorVpa;

    @Column(name = "target_payer_vpa", nullable = false)
    private String targetPayerVpa;

    @Column(name = "request_date", nullable = false)
    @Builder.Default
    private LocalDate requestDate = LocalDate.now();

    @Column(name = "request_count", nullable = false)
    @Builder.Default
    private Integer requestCount = 1;

    @Builder.Default
    private Boolean blocked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
