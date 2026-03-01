package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Monthly health report generated for each PSP.
 * Published to PSPs for transparency and relationship management.
 */
@Entity
@Table(name = "psp_health_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PspHealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "psp_id", nullable = false, length = 50)
    private String pspId;

    /** First day of the report month */
    @Column(name = "report_month", nullable = false)
    private LocalDate reportMonth;

    @Column(name = "total_transactions")
    @Builder.Default
    private Integer totalTransactions = 0;

    @Column(name = "successful_txns")
    @Builder.Default
    private Integer successfulTxns = 0;

    @Column(name = "failed_txns")
    @Builder.Default
    private Integer failedTxns = 0;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate;

    @Column(name = "total_volume_paisa")
    @Builder.Default
    private Long totalVolumePaisa = 0L;

    @Column(name = "avg_response_ms")
    private Integer avgResponseMs;

    @Column(name = "settlement_breaks")
    @Builder.Default
    private Integer settlementBreaks = 0;

    @Column(name = "fraud_flags")
    @Builder.Default
    private Integer fraudFlags = 0;

    @Column(name = "generated_at")
    @Builder.Default
    private Instant generatedAt = Instant.now();
}
