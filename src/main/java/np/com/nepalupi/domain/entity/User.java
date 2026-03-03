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

    // ── MPIN security (Section 9.2) ──
    @Column(name = "mpin_wrong_attempts")
    @Builder.Default
    private Integer mpinWrongAttempts = 0;

    @Column(name = "mpin_locked_until")
    private Instant mpinLockedUntil;

    // ── Account freeze fields (Fraud module) ──
    @Column(name = "is_frozen")
    @Builder.Default
    private Boolean isFrozen = false;

    @Column(name = "frozen_at")
    private Instant frozenAt;

    @Column(name = "freeze_reason")
    private String freezeReason;

    // ── App lock preference ──
    @Column(name = "app_lock_enabled")
    @Builder.Default
    private Boolean appLockEnabled = false;

    @Column(name = "app_lock_type")
    private String appLockType; // MPIN, BIOMETRIC, PATTERN

    // ── Preferred language ──
    @Column(name = "preferred_language")
    @Builder.Default
    private String preferredLanguage = "ne"; // Nepali default

    @Column(name = "email")
    private String email;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "fcm_token")
    private String fcmToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
