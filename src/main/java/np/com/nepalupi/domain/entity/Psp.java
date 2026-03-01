package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "psp")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Psp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "psp_id", unique = true, nullable = false)
    private String pspId;

    @Column(nullable = false)
    private String name;

    @Column(name = "api_key_hash", nullable = false)
    private String apiKeyHash;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ── Onboarding fields (V2 migration) ─────────────────────

    @Column(name = "nrb_license_number", length = 50)
    private String nrbLicenseNumber;

    @Column(name = "nrb_license_expiry")
    private LocalDate nrbLicenseExpiry;

    /** APPLICATION / LEGAL_AGREEMENT / TECHNICAL_CERTIFICATION / SECURITY_REVIEW / PILOT / PRODUCTION */
    @Column(name = "onboarding_stage", length = 30)
    @Builder.Default
    private String onboardingStage = "APPLICATION";

    /** 1=starter, 2=standard, 3=premium */
    @Column
    @Builder.Default
    private Integer tier = 1;

    @Column(name = "per_txn_limit_paisa")
    @Builder.Default
    private Long perTxnLimitPaisa = 1000000L;  // Rs 10,000 pilot default

    @Column(name = "daily_limit_paisa")
    @Builder.Default
    private Long dailyLimitPaisa = 10000000L;  // Rs 1,00,000 pilot default

    @Column(name = "pilot_start_date")
    private LocalDate pilotStartDate;

    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(name = "technical_contact_email", length = 200)
    private String technicalContactEmail;

    @Column(name = "technical_contact_phone", length = 20)
    private String technicalContactPhone;

    @Column(name = "client_cert_fingerprint", length = 200)
    private String clientCertFingerprint;

    @Column(name = "sandbox_token", length = 200)
    private String sandboxToken;

    @Column(name = "webhook_signing_secret", length = 200)
    private String webhookSigningSecret;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    // ── Timestamps ───────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
