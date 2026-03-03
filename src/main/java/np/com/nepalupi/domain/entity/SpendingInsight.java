package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spending_insights",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period_type", "period_value", "category"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SpendingInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "period_type", nullable = false, length = 20)
    private String periodType; // DAILY, WEEKLY, MONTHLY, YEARLY

    @Column(name = "period_value", nullable = false, length = 20)
    private String periodValue; // e.g. "2024-01", "2024-W05"

    @Column(length = 50)
    private String category; // FOOD, TRANSPORT, BILLS, TRANSFER, etc.

    @Column(name = "total_spent_paisa")
    @Builder.Default
    private Long totalSpentPaisa = 0L;

    @Column(name = "total_received_paisa")
    @Builder.Default
    private Long totalReceivedPaisa = 0L;

    @Column(name = "transaction_count")
    @Builder.Default
    private Integer transactionCount = 0;

    @Column(name = "avg_transaction_paisa")
    @Builder.Default
    private Long avgTransactionPaisa = 0L;

    @Column(name = "top_payee_vpa", length = 100)
    private String topPayeeVpa;

    @Column(name = "computed_at")
    @Builder.Default
    private Instant computedAt = Instant.now();
}
