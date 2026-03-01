package np.com.nepalupi.launch.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.launch.enums.GovtIntegrationStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
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

    @Column(name = "agency_code", nullable = false, unique = true)
    private String agencyCode;

    @Column(name = "payment_type", nullable = false)
    private String paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_status", nullable = false)
    @Builder.Default
    private GovtIntegrationStatus integrationStatus = GovtIntegrationStatus.IDENTIFIED;

    @Column(name = "estimated_annual_volume_paisa")
    private Long estimatedAnnualVolumePaisa;

    @Column(name = "estimated_annual_txn_count")
    private Long estimatedAnnualTxnCount;

    @Column(name = "technical_contact")
    private String technicalContact;

    @Column(name = "mou_signed_at")
    private Instant mouSignedAt;

    @Column(name = "integration_started_at")
    private Instant integrationStartedAt;

    @Column(name = "uat_completed_at")
    private Instant uatCompletedAt;

    @Column(name = "live_at")
    private Instant liveAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
