package np.com.nepalupi.launch.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.launch.enums.IncentiveProgramType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incentive_program")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IncentiveProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "program_name", nullable = false)
    private String programName;

    @Column(name = "program_code", nullable = false, unique = true)
    private String programCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type", nullable = false)
    private IncentiveProgramType programType;

    @Column(name = "psp_id")
    private String pspId;

    @Column(name = "budget_paisa", nullable = false)
    private Long budgetPaisa;

    @Builder.Default
    @Column(name = "spent_paisa")
    private Long spentPaisa = 0L;

    @Column(name = "per_user_limit_paisa")
    private Long perUserLimitPaisa;

    @Column(name = "per_txn_limit_paisa")
    private Long perTxnLimitPaisa;

    @Column(name = "min_txn_amount_paisa")
    private Long minTxnAmountPaisa;

    @Column(name = "cashback_percentage", precision = 5, scale = 2)
    private BigDecimal cashbackPercentage;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "total_redemptions")
    private Integer totalRedemptions = 0;

    @Builder.Default
    @Column(name = "unique_users")
    private Integer uniqueUsers = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
