package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "daily_transaction_stats")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DailyTransactionStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "stats_date", nullable = false)
    @Builder.Default
    private LocalDate statsDate = LocalDate.now();

    @Column(name = "total_amount_paisa", nullable = false)
    @Builder.Default
    private Long totalAmountPaisa = 0L;

    @Column(name = "transaction_count", nullable = false)
    @Builder.Default
    private Integer transactionCount = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
