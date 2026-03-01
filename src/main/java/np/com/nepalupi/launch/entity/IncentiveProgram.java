package np.com.nepalupi.launch.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.launch.enums.IncentiveProgramType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
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

    @Column(name = "psp_id")
    private String pspId;

    @Column(name = "program_name", nullable = false)
    private String programName;

    @Enumerated(EnumType.STRING)
    @Column(name = "program_type", nullable = false)
    private IncentiveProgramType programType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "budget_paisa")
    private Long budgetPaisa = 0L;

    @Builder.Default
    @Column(name = "spent_paisa")
    private Long spentPaisa = 0L;

    @Builder.Default
    @Column(name = "max_per_user_paisa")
    private Long maxPerUserPaisa = 0L;

    @Builder.Default
    @Column(name = "max_per_txn_paisa")
    private Long maxPerTxnPaisa = 0L;

    @Column(name = "eligible_txn_types")
    private String eligibleTxnTypes;

    @Builder.Default
    @Column(name = "min_txn_amount_paisa")
    private Long minTxnAmountPaisa = 0L;

    @Builder.Default
    @Column(name = "total_redemptions")
    private Long totalRedemptions = 0L;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
