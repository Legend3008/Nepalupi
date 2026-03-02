package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.OtpVerification;
import np.com.nepalupi.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Section 12.1.8: OTP (One-Time Password) service for registration, MPIN reset, device change.
 * <p>
 * Generates 6-digit OTPs, hashes before storage (never stored in plaintext),
 * enforces max 3 verification attempts, 5-minute expiry.
 * <p>
 * In production: integrates with Nepal Telecom / Ncell SMS API.
 * In dev: OTP logged to console for testing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final SmsGatewayService smsGatewayService;

    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 3;
    private static final int EXPIRY_MINUTES = 5;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate and send OTP to mobile number.
     *
     * @param mobileNumber Nepal mobile number (e.g., 9841000001)
     * @param purpose      REGISTRATION, MPIN_RESET, DEVICE_CHANGE
     * @return OTP ID for verification reference
     */
    @Transactional
    public String generateAndSend(String mobileNumber, String purpose) {
        // Invalidate any existing OTPs for this mobile + purpose
        otpRepository.invalidateExisting(mobileNumber, purpose, Instant.now());

        // Generate 6-digit OTP
        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        OtpVerification otpRecord = OtpVerification.builder()
                .mobileNumber(mobileNumber)
                .otpHash(otpHash)
                .purpose(purpose)
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .isVerified(false)
                .expiresAt(Instant.now().plusSeconds(EXPIRY_MINUTES * 60L))
                .build();

        otpRecord = otpRepository.save(otpRecord);

        // Send OTP via SMS gateway
        smsGatewayService.sendOtp(mobileNumber, otp, purpose);

        log.info("OTP generated for mobile=****{}, purpose={}, id={}",
                mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)),
                purpose, otpRecord.getId());

        return otpRecord.getId().toString();
    }

    /**
     * Verify OTP entered by user.
     *
     * @param mobileNumber Mobile number
     * @param otp          User-entered OTP
     * @param purpose      Purpose of OTP
     * @return true if OTP matches and is valid
     */
    @Transactional
    public boolean verify(String mobileNumber, String otp, String purpose) {
        Optional<OtpVerification> otpOpt = otpRepository
                .findLatestActiveOtp(mobileNumber, purpose, Instant.now());

        if (otpOpt.isEmpty()) {
            log.warn("No active OTP found for mobile=****{}, purpose={}",
                    mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)), purpose);
            return false;
        }

        OtpVerification otpRecord = otpOpt.get();

        // Check max attempts
        if (otpRecord.getAttempts() >= otpRecord.getMaxAttempts()) {
            log.warn("OTP max attempts exceeded for mobile=****{}, purpose={}",
                    mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)), purpose);
            return false;
        }

        // Increment attempts
        otpRecord.setAttempts(otpRecord.getAttempts() + 1);

        // Check hash match
        String inputHash = hashOtp(otp);
        if (inputHash.equals(otpRecord.getOtpHash())) {
            otpRecord.setIsVerified(true);
            otpRecord.setVerifiedAt(Instant.now());
            otpRepository.save(otpRecord);
            log.info("OTP verified successfully for mobile=****{}, purpose={}",
                    mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)), purpose);
            return true;
        }

        otpRepository.save(otpRecord);
        log.warn("OTP verification failed (attempt {}/{}) for mobile=****{}, purpose={}",
                otpRecord.getAttempts(), otpRecord.getMaxAttempts(),
                mobileNumber.substring(Math.max(0, mobileNumber.length() - 4)), purpose);
        return false;
    }

    /**
     * Check if a verified OTP exists for the given mobile + purpose (within last 10 minutes).
     */
    public boolean hasVerifiedOtp(String mobileNumber, String purpose) {
        return otpRepository.existsVerifiedOtp(mobileNumber, purpose,
                Instant.now().minusSeconds(600));
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000; // 100000-999999
        return String.valueOf(otp);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash OTP", e);
        }
    }
}
