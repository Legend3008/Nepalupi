package np.com.nepalupi.launch.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.launch.enums.LaunchPhaseName;
import np.com.nepalupi.launch.enums.LaunchPhaseStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "launch_phase")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LaunchPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phase_number", nullable = false)
    private Integer phaseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase_name", nullable = false)
    private LaunchPhaseName phaseName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private LaunchPhaseStatus status = LaunchPhaseStatus.NOT_STARTED;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "registration_daily_cap")
    private Integer registrationDailyCap;

    @Column(name = "per_txn_limit_paisa")
    private Long perTxnLimitPaisa;

    @Column(name = "daily_limit_paisa")
    private Long dailyLimitPaisa;

    @Column(name = "target_banks")
    private Integer targetBanks;

    @Column(name = "target_psp_apps")
    private Integer targetPspApps;

    @Column(name = "target_merchants")
    private Integer targetMerchants;

    @Column(name = "target_registered_users")
    private Integer targetRegisteredUsers;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
