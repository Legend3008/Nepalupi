package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Daily transaction summary report for NRB submission.
 * Generated at 9 AM for the previous day.
 */
@Entity
@Table(name = "nrb_daily_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NrbDailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "report_date", unique = true, nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_txn_count", nullable = false)
    @Builder.Default
    private Integer totalTxnCount = 0;

    @Column(name = "total_txn_value_paisa", nullable = false)
    @Builder.Default
    private Long totalTxnValuePaisa = 0L;

    @Column(name = "p2p_count")
    @Builder.Default
    private Integer p2pCount = 0;

    @Column(name = "p2p_value_paisa")
    @Builder.Default
    private Long p2pValuePaisa = 0L;

    @Column(name = "p2m_count")
    @Builder.Default
    private Integer p2mCount = 0;

    @Column(name = "p2m_value_paisa")
    @Builder.Default
    private Long p2mValuePaisa = 0L;

    @Column(name = "collect_count")
    @Builder.Default
    private Integer collectCount = 0;

    @Column(name = "collect_value_paisa")
    @Builder.Default
    private Long collectValuePaisa = 0L;

    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;

    /** JSON map: {"INSUFFICIENT_FUNDS": 12, "TIMEOUT": 3, ...} */
    @Column(name = "failure_reasons", columnDefinition = "jsonb")
    private String failureReasons;

    @Column(name = "reversal_count")
    @Builder.Default
    private Integer reversalCount = 0;

    @Column(name = "reversal_value_paisa")
    @Builder.Default
    private Long reversalValuePaisa = 0L;

    /** JSON per-bank net settlement position */
    @Column(name = "net_settlement_position", columnDefinition = "jsonb")
    private String netSettlementPosition;

    @Column
    @Builder.Default
    private Boolean submitted = false;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;
}
