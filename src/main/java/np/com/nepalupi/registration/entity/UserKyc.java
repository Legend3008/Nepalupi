package np.com.nepalupi.registration.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.registration.enums.KycDocumentType;
import np.com.nepalupi.registration.enums.KycLevel;
import np.com.nepalupi.registration.enums.KycVerificationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_kyc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserKyc {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", nullable = false)
    private KycLevel kycLevel = KycLevel.MINIMUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private KycDocumentType documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "document_image_hash")
    private String documentImageHash;

    @Column(name = "selfie_image_hash")
    private String selfieImageHash;

    @Column(name = "ocr_extracted_data", columnDefinition = "jsonb")
    private String ocrExtractedData;

    @Column(name = "face_match_score")
    private BigDecimal faceMatchScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private KycVerificationStatus verificationStatus = KycVerificationStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "document_expiry")
    private LocalDate documentExpiry;

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
