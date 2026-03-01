package np.com.nepalupi.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_performance_metric", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bank_code", "metric_date"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BankPerformanceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Builder.Default
    @Column(name = "total_transactions")
    private Long totalTransactions = 0L;

    @Builder.Default
    @Column(name = "successful_transactions")
    private Long successfulTransactions = 0L;

    @Builder.Default
    @Column(name = "failed_transactions")
    private Long failedTransactions = 0L;

    @Builder.Default
    @Column(name = "timeout_count")
    private Long timeoutCount = 0L;

    @Builder.Default
    @Column(name = "avg_response_time_ms")
    private Long avgResponseTimeMs = 0L;

    @Builder.Default
    @Column(name = "p95_response_time_ms")
    private Long p95ResponseTimeMs = 0L;

    @Builder.Default
    @Column(name = "p99_response_time_ms")
    private Long p99ResponseTimeMs = 0L;

    @Builder.Default
    @Column(name = "settlement_accuracy_pct")
    private BigDecimal settlementAccuracyPct = new BigDecimal("100.00");

    @Builder.Default
    @Column(name = "error_rate_pct")
    private BigDecimal errorRatePct = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "performance_notice_sent")
    private Boolean performanceNoticeSent = false;

    @Column(name = "performance_notice_sent_at")
    private Instant performanceNoticeSentAt;

    @Builder.Default
    @Column(name = "below_network_average")
    private Boolean belowNetworkAverage = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
