package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.BankAccountRepository;
import np.com.nepalupi.repository.UserRepository;
import np.com.nepalupi.service.pin.PinEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Section 9.2 / 12: MPIN (UPI PIN) reset flow.
 * <p>
 * Reset flow:
 * 1. User requests MPIN reset via PSP app
 * 2. OTP sent to registered mobile
 * 3. User verifies OTP
 * 4. User enters debit card last 6 digits + expiry (re-authentication)
 * 5. New MPIN set
 * <p>
 * 3 wrong MPIN attempts → locked for 24 hours (Section 9.2).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MpinResetService {

    private final OtpService otpService;
    private final MpinService mpinService;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    private static final int MAX_WRONG_ATTEMPTS = 3;
    private static final long LOCK_DURATION_HOURS = 24;

    /**
     * Step 1: Initiate MPIN reset — send OTP.
     */
    @Transactional
    public Map<String, String> initiateReset(String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found for mobile: " + mobileNumber));

        // Check if MPIN is locked
        if (user.getMpinLockedUntil() != null &&
                user.getMpinLockedUntil().isAfter(java.time.Instant.now())) {
            long remainingMinutes = java.time.Duration.between(
                    java.time.Instant.now(), user.getMpinLockedUntil()).toMinutes();
            throw new IllegalStateException(
                    "MPIN is locked. Try again in " + remainingMinutes + " minutes.");
        }

        String otpId = otpService.generateAndSend(mobileNumber, "MPIN_RESET");

        log.info("MPIN reset initiated for user={}, otpId={}", user.getId(), otpId);
        return Map.of(
                "status", "OTP_SENT",
                "otpId", otpId,
                "message", "OTP sent to registered mobile number"
        );
    }

    /**
     * Step 2: Verify OTP and card, then set new MPIN.
     */
    @Transactional
    public Map<String, String> completeReset(String mobileNumber, String otp,
                                               String cardLast6, String cardExpiry,
                                               String newMpin) {
        // Verify OTP
        boolean otpValid = otpService.verify(mobileNumber, otp, "MPIN_RESET");
        if (!otpValid) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Look up primary bank account for the user
        BankAccount primaryAccount = bankAccountRepository.findByUserIdAndIsPrimary(user.getId(), true)
                .orElseThrow(() -> new IllegalArgumentException("No primary bank account found for user"));

        // Re-verify card details via card validation flow (debit card last 6 + expiry)
        try {
            mpinService.initiateCardValidation(
                    user.getId(), primaryAccount.getBankCode(), primaryAccount.getAccountNumber(), cardLast6, cardExpiry);
        } catch (Exception e) {
            throw new IllegalArgumentException("Card verification failed: " + e.getMessage());
        }

        // Validate new MPIN format (4-6 digits)
        if (!newMpin.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("MPIN must be 4-6 digits");
        }

        // Reset wrong attempt counter and set new MPIN
        user.setMpinWrongAttempts(0);
        user.setMpinLockedUntil(null);
        userRepository.save(user);

        // Set new MPIN via MpinService (encrypts and stores at bank)
        mpinService.setMpin(user.getId(), primaryAccount.getBankCode(), primaryAccount.getAccountNumber(), newMpin);

        log.info("MPIN reset completed for user={}", user.getId());
        return Map.of(
                "status", "MPIN_RESET_SUCCESS",
                "message", "MPIN has been reset successfully"
        );
    }

    /**
     * Record a wrong MPIN attempt. Lock after MAX_WRONG_ATTEMPTS.
     */
    @Transactional
    public void recordWrongAttempt(UUID userId) {
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) return;

        int attempts = (user.getMpinWrongAttempts() != null ? user.getMpinWrongAttempts() : 0) + 1;
        user.setMpinWrongAttempts(attempts);

        if (attempts >= MAX_WRONG_ATTEMPTS) {
            user.setMpinLockedUntil(java.time.Instant.now().plusSeconds(LOCK_DURATION_HOURS * 3600));
            log.warn("MPIN LOCKED for 24h: user={}, wrongAttempts={}", userId, attempts);
        }

        userRepository.save(user);
    }

    /**
     * Check if MPIN is currently locked for a user.
     */
    public boolean isMpinLocked(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getMpinLockedUntil() != null &&
                          u.getMpinLockedUntil().isAfter(java.time.Instant.now()))
                .orElse(false);
    }
}
