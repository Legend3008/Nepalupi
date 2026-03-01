package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.registration.dto.KycSubmissionRequest;
import np.com.nepalupi.registration.entity.UserKyc;
import np.com.nepalupi.registration.enums.KycDocumentType;
import np.com.nepalupi.registration.enums.KycLevel;
import np.com.nepalupi.registration.enums.KycVerificationStatus;
import np.com.nepalupi.registration.repository.UserKycRepository;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * KYC Service — Minimum & Full KYC verification.
 * <p>
 * Indian UPI tiered KYC model adapted for Nepal (NRB guidelines):
 * <p>
 * Minimum KYC (self-declared):
 *   - Name, DOB, address from user input
 *   - Daily limit: NPR 25,000
 *   - Auto-approved
 * <p>
 * Full KYC (document-verified):
 *   - Document scan (Citizenship, Passport, or National ID)
 *   - Selfie
 *   - OCR extraction + face match
 *   - Daily limit: NPR 200,000
 *   - Auto-approved if face-match > 80%, else manual review
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final UserKycRepository userKycRepository;
    private final UserRepository userRepository;

    private static final BigDecimal FACE_MATCH_AUTO_APPROVE_THRESHOLD = new BigDecimal("80.00");
    private static final long MINIMUM_KYC_DAILY_LIMIT = 2_500_000L;  // Rs 25,000
    private static final long FULL_KYC_DAILY_LIMIT = 20_000_000L;     // Rs 200,000

    /**
     * Submit Minimum KYC — just user-declared info, auto-approved.
     */
    @Transactional
    public UserKyc submitMinimumKyc(KycSubmissionRequest request) {
        log.info("Processing minimum KYC for user={}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserKyc kyc = UserKyc.builder()
                .userId(user.getId())
                .kycLevel(KycLevel.MINIMUM)
                .verificationStatus(KycVerificationStatus.AUTO_APPROVED)
                .verifiedBy("SYSTEM")
                .verifiedAt(Instant.now())
                .build();

        kyc = userKycRepository.save(kyc);

        user.setKycLevel("MINIMUM");
        user.setKycStatus("MINIMUM_KYC_DONE");
        user.setDailyLimitPaisa(MINIMUM_KYC_DAILY_LIMIT);
        userRepository.save(user);

        log.info("Minimum KYC auto-approved for user={}", user.getId());
        return kyc;
    }

    /**
     * Submit Full KYC — document + selfie verification with OCR & face match.
     */
    @Transactional
    public UserKyc submitFullKyc(KycSubmissionRequest request) {
        log.info("Processing full KYC for user={}", request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        KycDocumentType docType = KycDocumentType.valueOf(request.getDocumentType());

        // Hash the document and selfie images (never store raw images)
        String docHash = hashImage(request.getDocumentImageBase64());
        String selfieHash = hashImage(request.getSelfieImageBase64());

        // Simulate OCR extraction
        String ocrData = performOcr(request.getDocumentImageBase64(), docType);

        // Simulate face match (document photo vs selfie)
        BigDecimal faceMatchScore = performFaceMatch(docHash, selfieHash);

        // Determine verification outcome
        KycVerificationStatus status;
        if (faceMatchScore.compareTo(FACE_MATCH_AUTO_APPROVE_THRESHOLD) >= 0) {
            status = KycVerificationStatus.AUTO_APPROVED;
        } else {
            status = KycVerificationStatus.MANUAL_REVIEW;
            log.warn("KYC for user={} requires manual review (face match={}/100)",
                    user.getId(), faceMatchScore);
        }

        UserKyc kyc = UserKyc.builder()
                .userId(user.getId())
                .kycLevel(KycLevel.FULL)
                .documentType(docType)
                .documentNumber(request.getDocumentNumber())
                .documentImageHash(docHash)
                .selfieImageHash(selfieHash)
                .ocrExtractedData(ocrData)
                .faceMatchScore(faceMatchScore)
                .verificationStatus(status)
                .verifiedBy(status == KycVerificationStatus.AUTO_APPROVED ? "SYSTEM" : null)
                .verifiedAt(status == KycVerificationStatus.AUTO_APPROVED ? Instant.now() : null)
                .build();

        kyc = userKycRepository.save(kyc);

        if (status == KycVerificationStatus.AUTO_APPROVED) {
            user.setKycLevel("FULL");
            user.setKycStatus("FULL_KYC_DONE");
            user.setDailyLimitPaisa(FULL_KYC_DAILY_LIMIT);
            userRepository.save(user);
            log.info("Full KYC auto-approved for user={}, face match={}", user.getId(), faceMatchScore);
        }

        return kyc;
    }

    /**
     * Manual review approval by operations staff.
     */
    @Transactional
    public UserKyc approveKyc(java.util.UUID kycId, String approverName) {
        UserKyc kyc = userKycRepository.findById(kycId)
                .orElseThrow(() -> new IllegalArgumentException("KYC record not found"));

        if (kyc.getVerificationStatus() != KycVerificationStatus.MANUAL_REVIEW) {
            throw new IllegalStateException("KYC not in MANUAL_REVIEW state");
        }

        kyc.setVerificationStatus(KycVerificationStatus.APPROVED);
        kyc.setVerifiedBy(approverName);
        kyc.setVerifiedAt(Instant.now());
        kyc = userKycRepository.save(kyc);

        User user = userRepository.findById(kyc.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setKycLevel(kyc.getKycLevel().name());
        user.setKycStatus("FULL_KYC_DONE");
        user.setDailyLimitPaisa(FULL_KYC_DAILY_LIMIT);
        userRepository.save(user);

        log.info("KYC manually approved for user={} by {}", kyc.getUserId(), approverName);
        return kyc;
    }

    /**
     * Reject KYC submission.
     */
    @Transactional
    public UserKyc rejectKyc(java.util.UUID kycId, String reason, String approverName) {
        UserKyc kyc = userKycRepository.findById(kycId)
                .orElseThrow(() -> new IllegalArgumentException("KYC record not found"));

        kyc.setVerificationStatus(KycVerificationStatus.REJECTED);
        kyc.setRejectionReason(reason);
        kyc.setVerifiedBy(approverName);
        kyc.setVerifiedAt(Instant.now());
        return userKycRepository.save(kyc);
    }

    // ── Helpers ──

    private String hashImage(String base64Image) {
        if (base64Image == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Base64.getDecoder().decode(base64Image));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String performOcr(String documentBase64, KycDocumentType docType) {
        // In production: call OCR service (e.g., Google Vision, AWS Textract)
        return "{\"name\": \"extracted\", \"dob\": \"1990-01-01\", \"documentType\": \"" + docType + "\"}";
    }

    private BigDecimal performFaceMatch(String docHash, String selfieHash) {
        // In production: call face-match ML service
        // Simulated: return high score for demo
        return new BigDecimal("92.50");
    }
}
