package np.com.nepalupi.mandate.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.mandate.enums.MandateCategory;
import np.com.nepalupi.mandate.enums.MandateFrequency;
import np.com.nepalupi.mandate.enums.MandateStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "mandate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Mandate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mandate_ref", unique = true, nullable = false)
    private String mandateRef;

    @Column(name = "merchant_vpa", nullable = false)
    private String merchantVpa;

    @Column(name = "payer_vpa", nullable = false)
    private String payerVpa;

    @Column(name = "amount_paisa")
    private Long amountPaisa;  // Fixed amount (null if variable)

    @Column(name = "max_amount_paisa", nullable = false)
    private Long maxAmountPaisa;  // Ceiling

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MandateFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MandateCategory category;

    @Column(name = "mandate_type", nullable = false)
    @Builder.Default
    private String mandateType = "RECURRING";  // RECURRING / ONE_TIME

    private String purpose;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_debit_date")
    private LocalDate nextDebitDate;

    @Column(name = "last_debit_date")
    private LocalDate lastDebitDate;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MandateStatus status = MandateStatus.PENDING_APPROVAL;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cooling_period_minutes")
    @Builder.Default
    private Integer coolingPeriodMinutes = 0;

    @Column(name = "cooling_ends_at")
    private Instant coolingEndsAt;

    @Column(name = "merchant_psp_id")
    private String merchantPspId;

    @Column(name = "payer_psp_id")
    private String payerPspId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
