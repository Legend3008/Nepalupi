package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Monthly volume report for NRB — user counts, VPA registrations, PSP activity.
 */
@Entity
@Table(name = "nrb_monthly_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NrbMonthlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** First day of the month (e.g. 2026-01-01) */
    @Column(name = "report_month", unique = true, nullable = false)
    private LocalDate reportMonth;

    @Column(name = "total_txn_count", nullable = false)
    @Builder.Default
    private Integer totalTxnCount = 0;

    @Column(name = "total_txn_value_paisa", nullable = false)
    @Builder.Default
    private Long totalTxnValuePaisa = 0L;

    @Column(name = "registered_vpa_count")
    @Builder.Default
    private Integer registeredVpaCount = 0;

    @Column(name = "active_psp_count")
    @Builder.Default
    private Integer activePspCount = 0;

    @Column(name = "active_user_count")
    @Builder.Default
    private Integer activeUserCount = 0;

    @Column(name = "new_vpa_registrations")
    @Builder.Default
    private Integer newVpaRegistrations = 0;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;
}
