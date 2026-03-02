package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Category-based transaction limit — NRB-mandated limits per transaction category.
 * Different categories (P2P, P2M, BILL_PAYMENT, INSURANCE, etc.) can have different
 * per-transaction and daily aggregate limits.
 */
@Entity
@Table(name = "category_transaction_limit")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CategoryTransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String category;

    @Column(name = "per_txn_limit_paisa", nullable = false)
    private Long perTxnLimitPaisa;

    @Column(name = "daily_limit_paisa", nullable = false)
    private Long dailyLimitPaisa;

    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
