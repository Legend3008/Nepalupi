package np.com.nepalupi.merchant.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.merchant.enums.SettlementStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "merchant_settlement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(name = "total_txn_count")
    @Builder.Default
    private Integer totalTxnCount = 0;

    @Column(name = "total_amount_paisa")
    @Builder.Default
    private Long totalAmountPaisa = 0L;

    @Column(name = "mdr_deducted_paisa")
    @Builder.Default
    private Long mdrDeductedPaisa = 0L;

    @Column(name = "net_amount_paisa")
    @Builder.Default
    private Long netAmountPaisa = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status")
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @Column(name = "settlement_reference")
    private String settlementReference;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
