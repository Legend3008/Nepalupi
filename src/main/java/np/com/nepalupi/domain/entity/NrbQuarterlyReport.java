package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Quarterly risk & fraud report for NRB.
 */
@Entity
@Table(name = "nrb_quarterly_report")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NrbQuarterlyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** E.g. "2026-Q1" */
    @Column(name = "report_quarter", unique = true, nullable = false, length = 10)
    private String reportQuarter;

    @Column(name = "fraud_incident_count")
    @Builder.Default
    private Integer fraudIncidentCount = 0;

    @Column(name = "fraud_total_value_paisa")
    @Builder.Default
    private Long fraudTotalValuePaisa = 0L;

    /** JSON breakdown by fraud type */
    @Column(name = "fraud_types", columnDefinition = "jsonb")
    private String fraudTypes;

    /** JSON fraud resolution summary */
    @Column(name = "fraud_resolution_summary", columnDefinition = "jsonb")
    private String fraudResolutionSummary;

    @Column(name = "system_downtime_minutes")
    @Builder.Default
    private Integer systemDowntimeMinutes = 0;

    /** JSON [{start, end, cause, resolution}] */
    @Column(name = "downtime_incidents", columnDefinition = "jsonb")
    private String downtimeIncidents;

    @Column(name = "security_incidents")
    @Builder.Default
    private Integer securityIncidents = 0;

    @Column(name = "str_filed_count")
    @Builder.Default
    private Integer strFiledCount = 0;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private Instant generatedAt;
}
