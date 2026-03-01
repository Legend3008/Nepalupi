package np.com.nepalupi.launch.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.launch.enums.AcquisitionChannel;
import np.com.nepalupi.launch.enums.FootfallCategory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_acquisition")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MerchantAcquisition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "category")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "footfall_category")
    private FootfallCategory footfallCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_channel")
    private AcquisitionChannel acquisitionChannel;

    @Column(name = "acquired_by")
    private String acquiredBy;

    @Builder.Default
    @Column(name = "qr_deployed")
    private Boolean qrDeployed = false;

    @Column(name = "qr_deployed_at")
    private Instant qrDeployedAt;

    @Column(name = "first_transaction_at")
    private Instant firstTransactionAt;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = false;

    @Builder.Default
    @Column(name = "monthly_txn_count")
    private Integer monthlyTxnCount = 0;

    @Builder.Default
    @Column(name = "monthly_volume_paisa")
    private Long monthlyVolumePaisa = 0L;

    @Column(name = "onboarded_at")
    private Instant onboardedAt;

    @Column(name = "churned_at")
    private Instant churnedAt;

    @Column(name = "churn_reason", columnDefinition = "TEXT")
    private String churnReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
