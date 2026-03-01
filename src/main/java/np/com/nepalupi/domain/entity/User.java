package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mobile_number", unique = true, nullable = false)
    private String mobileNumber;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "kyc_status", nullable = false)
    @Builder.Default
    private String kycStatus = "PENDING";

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ── Registration & KYC fields (Module 9) ──
    @Column(name = "phone_number_verified")
    @Builder.Default
    private Boolean phoneNumberVerified = false;

    @Column(name = "device_bound")
    @Builder.Default
    private Boolean deviceBound = false;

    @Column(name = "kyc_level")
    @Builder.Default
    private String kycLevel = "NONE";

    @Column(name = "daily_limit_paisa")
    @Builder.Default
    private Long dailyLimitPaisa = 2500000L;  // Rs 25,000 default

    @Column(name = "mpin_set")
    @Builder.Default
    private Boolean mpinSet = false;

    @Column(name = "last_device_change_at")
    private Instant lastDeviceChangeAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
