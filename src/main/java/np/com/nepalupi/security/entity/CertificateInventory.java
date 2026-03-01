package np.com.nepalupi.security.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.security.enums.CertificateStatus;
import np.com.nepalupi.security.enums.CertificateType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificate_inventory")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CertificateInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cert_name", nullable = false)
    private String certName;

    @Enumerated(EnumType.STRING)
    @Column(name = "cert_type", nullable = false)
    private CertificateType certType;

    @Column(name = "subject_cn")
    private String subjectCn;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "fingerprint_sha256")
    private String fingerprintSha256;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Builder.Default
    @Column(name = "auto_rotate")
    private Boolean autoRotate = false;

    @Column(name = "rotation_period_days")
    private Integer rotationPeriodDays;

    @Column(name = "last_rotated_at")
    private Instant lastRotatedAt;

    @Column(name = "next_rotation_due")
    private Instant nextRotationDue;

    @Column(name = "associated_service")
    private String associatedService;

    @Column(name = "key_store_location")
    private String keyStoreLocation;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private CertificateStatus status = CertificateStatus.ACTIVE;

    @Builder.Default
    @Column(name = "alert_sent")
    private Boolean alertSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
