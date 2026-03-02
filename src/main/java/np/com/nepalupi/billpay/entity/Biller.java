package np.com.nepalupi.billpay.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Biller — registered entity in the BBPS (Bharat Bill Payment System) equivalent.
 * Billers include utilities (electricity, water, telecom), insurance companies,
 * educational institutions, government agencies, etc.
 */
@Entity
@Table(name = "biller")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Biller {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "biller_id", unique = true, nullable = false)
    private String billerId;

    @Column(name = "biller_name", nullable = false)
    private String billerName;

    @Column(nullable = false)
    private String category; // ELECTRICITY, WATER, TELECOM, GAS, INSURANCE, EDUCATION, TAX, BROADBAND

    @Column(name = "sub_category")
    private String subCategory;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "settlement_account", nullable = false)
    private String settlementAccount;

    @Column(name = "is_adhoc")
    @Builder.Default
    private Boolean isAdhoc = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "fetch_supported")
    @Builder.Default
    private Boolean fetchSupported = false;

    @Column(name = "payment_modes")
    @Builder.Default
    private String paymentModes = "ONLINE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
