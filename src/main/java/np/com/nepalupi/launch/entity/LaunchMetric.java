package np.com.nepalupi.launch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "launch_metric", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"metric_date"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LaunchMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "metric_date", nullable = false, unique = true)
    private LocalDate metricDate;

    // User metrics
    @Builder.Default
    @Column(name = "total_registered_users")
    private Long totalRegisteredUsers = 0L;

    @Builder.Default
    @Column(name = "new_registrations_today")
    private Long newRegistrationsToday = 0L;

    @Builder.Default
    @Column(name = "active_users_30d")
    private Long activeUsers30d = 0L;

    // Transaction metrics
    @Builder.Default
    @Column(name = "total_transactions_today")
    private Long totalTransactionsToday = 0L;

    @Builder.Default
    @Column(name = "total_volume_paisa_today")
    private Long totalVolumePaisaToday = 0L;

    @Builder.Default
    @Column(name = "txn_success_rate_pct")
    private BigDecimal txnSuccessRatePct = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "avg_txn_amount_paisa")
    private Long avgTxnAmountPaisa = 0L;

    @Builder.Default
    @Column(name = "p2p_count")
    private Long p2pCount = 0L;

    @Builder.Default
    @Column(name = "p2m_count")
    private Long p2mCount = 0L;

    // Merchant metrics
    @Builder.Default
    @Column(name = "total_active_merchants")
    private Long totalActiveMerchants = 0L;

    @Builder.Default
    @Column(name = "new_merchants_today")
    private Long newMerchantsToday = 0L;

    // Bank coverage
    @Builder.Default
    @Column(name = "total_banks_live")
    private Integer totalBanksLive = 0;

    @Builder.Default
    @Column(name = "banking_coverage_pct")
    private BigDecimal bankingCoveragePct = new BigDecimal("0.00");

    // Settlement
    @Builder.Default
    @Column(name = "settlement_accuracy_pct")
    private BigDecimal settlementAccuracyPct = new BigDecimal("100.00");

    @Builder.Default
    @Column(name = "reconciliation_breaks")
    private Integer reconciliationBreaks = 0;

    // PSP
    @Builder.Default
    @Column(name = "total_psp_apps_live")
    private Integer totalPspAppsLive = 0;

    // Regional merchants
    @Builder.Default
    @Column(name = "kathmandu_merchants")
    private Integer kathmanduMerchants = 0;

    @Builder.Default
    @Column(name = "pokhara_merchants")
    private Integer pokharaMerchants = 0;

    @Builder.Default
    @Column(name = "biratnagar_merchants")
    private Integer biratnagarMerchants = 0;

    @Builder.Default
    @Column(name = "other_merchants")
    private Integer otherMerchants = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
