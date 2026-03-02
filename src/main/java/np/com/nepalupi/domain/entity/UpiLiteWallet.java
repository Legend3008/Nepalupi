package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upi_lite_wallet")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpiLiteWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;

    @Column(name = "balance_paisa", nullable = false)
    @Builder.Default
    private Long balancePaisa = 0L;

    @Column(name = "max_balance_paisa", nullable = false)
    @Builder.Default
    private Long maxBalancePaisa = 200000L;

    @Column(name = "per_txn_limit_paisa", nullable = false)
    @Builder.Default
    private Long perTxnLimitPaisa = 50000L;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "linked_bank_account_id", nullable = false)
    private UUID linkedBankAccountId;

    @Column(name = "linked_bank_code", nullable = false)
    private String linkedBankCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
