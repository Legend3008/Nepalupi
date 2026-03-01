package np.com.nepalupi.launch.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.launch.enums.GovtIntegrationStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "govt_payment_integration")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GovtPaymentIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agency_name", nullable = false)
    private String agencyName;

    @Column(name = "payment_type", nullable = false)
    private String paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_status", nullable = false)
    @Builder.Default
    private GovtIntegrationStatus integrationStatus = GovtIntegrationStatus.IDENTIFIED;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_email")
    private String contactEmail;

    @Builder.Default
    @Column(name = "estimated_monthly_volume")
    private Long estimatedMonthlyVolume = 0L;

    @Builder.Default
    @Column(name = "estimated_monthly_amount_paisa")
    private Long estimatedMonthlyAmountPaisa = 0L;

    @Column(name = "agreement_signed_at")
    private Instant agreementSignedAt;

    @Column(name = "go_live_date")
    private LocalDate goLiveDate;

    @Builder.Default
    @Column(name = "actual_monthly_volume")
    private Long actualMonthlyVolume = 0L;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
