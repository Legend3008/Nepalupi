package np.com.nepalupi.mandate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collect_block_list")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CollectBlockList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payer_vpa", nullable = false)
    private String payerVpa;

    @Column(name = "blocked_vpa", nullable = false)
    private String blockedVpa;

    private String reason;

    @Column(name = "blocked_at")
    @Builder.Default
    private Instant blockedAt = Instant.now();
}
