package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_on_upi_cards")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CreditOnUpiCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_issuer", nullable = false, length = 100)
    private String cardIssuer;

    @Column(name = "card_last_four", nullable = false, length = 4)
    private String cardLastFour;

    @Column(name = "card_network", length = 20)
    private String cardNetwork;

    @Column(name = "linked_vpa", length = 100)
    private String linkedVpa;

    @Column(name = "credit_limit_paisa")
    private Long creditLimitPaisa;

    @Column(name = "available_limit_paisa")
    private Long availableLimitPaisa;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "linked_at")
    @Builder.Default
    private Instant linkedAt = Instant.now();

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
