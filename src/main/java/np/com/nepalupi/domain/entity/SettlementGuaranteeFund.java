package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Settlement Guarantee Fund — NRB-mandated fund maintained by participating banks
 * to guarantee inter-bank UPI settlement. Each bank contributes a proportional amount
 * based on their UPI transaction volume. The fund is used to cover settlement defaults.
 */
@Entity
@Table(name = "settlement_guarantee_fund")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SettlementGuaranteeFund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "contribution_paisa", nullable = false)
    private Long contributionPaisa;

    @Column(name = "fund_date", nullable = false)
    private LocalDate fundDate;

    @Column(name = "total_fund_paisa", nullable = false)
    private Long totalFundPaisa;

    @Column(name = "utilization_paisa")
    @Builder.Default
    private Long utilizationPaisa = 0L;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, UTILIZED, REPLENISHED

    @Column(name = "nrb_approved")
    @Builder.Default
    private Boolean nrbApproved = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
