package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * TPAP — Third Party Application Provider.
 * <p>
 * In the UPI model, a TPAP uses another PSP's banking license (sponsor PSP)
 * to provide UPI payment services. Example: Google Pay (TPAP) uses Axis Bank (sponsor PSP).
 * The TPAP does NOT directly connect to NCHL — all routing goes through the sponsor PSP.
 */
@Entity
@Table(name = "tpap")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Tpap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tpap_id", unique = true, nullable = false)
    private String tpapId;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_psp_id", nullable = false)
    private Psp sponsorPsp;

    @Column(name = "sponsor_bank_code", nullable = false)
    private String sponsorBankCode;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING_APPROVAL";

    @Column(name = "nrb_license_number")
    private String nrbLicenseNumber;

    @Column(name = "nrb_license_expiry")
    private LocalDate nrbLicenseExpiry;

    @Column(name = "technical_contact_email")
    private String technicalContactEmail;

    @Column(name = "technical_contact_phone")
    private String technicalContactPhone;

    @Column(name = "api_key_hash")
    private String apiKeyHash;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
