package np.com.nepalupi.merchant.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.merchant.enums.MerchantCategory;
import np.com.nepalupi.merchant.enums.MerchantStatus;
import np.com.nepalupi.merchant.enums.MerchantType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", unique = true, nullable = false)
    private String merchantId;  // "MER-TEASTALL-12345"

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "psp_id")
    private String pspId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_category", nullable = false)
    private MerchantCategory businessCategory;

    @Column(name = "mcc_code")
    private String mccCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_type", nullable = false)
    private MerchantType merchantType = MerchantType.SMALL;

    @Column(name = "merchant_vpa", unique = true)
    private String merchantVpa;

    // Address
    @Column(name = "address_line")
    private String addressLine;

    private String city;
    private String district;

    // Documents (large merchants)
    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "registration_doc_hash")
    private String registrationDocHash;

    // QR
    @Column(name = "static_qr_data", columnDefinition = "TEXT")
    private String staticQrData;

    // API access (large merchants)
    @Column(name = "api_key_hash")
    private String apiKeyHash;

    @Column(name = "api_secret_hash")
    private String apiSecretHash;

    @Column(name = "webhook_url")
    private String webhookUrl;

    // Settlement
    @Column(name = "settlement_account_id")
    private UUID settlementAccountId;

    @Column(name = "settlement_cycle")
    @Builder.Default
    private String settlementCycle = "T1";

    @Column(name = "mdr_percent")
    @Builder.Default
    private BigDecimal mdrPercent = BigDecimal.ZERO;

    // Notifications
    @Column(name = "push_enabled")
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "audio_notification")
    @Builder.Default
    private Boolean audioNotification = true;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MerchantStatus status = MerchantStatus.PENDING;

    @Column(name = "suspended_reason")
    private String suspendedReason;

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
