package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MPIN management and verification API.
 */
@RestController
@RequestMapping("/api/v1/mpin")
@RequiredArgsConstructor
public class MpinController {

    private final np.com.nepalupi.service.pin.PinEncryptionService pinEncryptionService;
    private final UserRepository userRepository;

    /**
     * Verify MPIN for standalone PIN verification (before sensitive operations).
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPin(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        String mpin = request.get("mpin");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if MPIN is locked
        if (user.getMpinLockedUntil() != null
                && user.getMpinLockedUntil().isAfter(java.time.Instant.now())) {
            return ResponseEntity.status(423).body(Map.of(
                    "verified", false,
                    "locked", true,
                    "lockedUntil", user.getMpinLockedUntil().toString(),
                    "message", "MPIN is locked. Try again later."
            ));
        }

        // Verify PIN
        boolean valid = pinEncryptionService.verifyPin(userId, mpin);

        if (valid) {
            // Reset wrong attempts on success
            user.setMpinWrongAttempts(0);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("verified", true));
        } else {
            // Increment wrong attempts
            int attempts = (user.getMpinWrongAttempts() != null ? user.getMpinWrongAttempts() : 0) + 1;
            user.setMpinWrongAttempts(attempts);

            // Lock after 3 wrong attempts for 30 minutes
            if (attempts >= 3) {
                user.setMpinLockedUntil(java.time.Instant.now().plusSeconds(1800));
                userRepository.save(user);
                return ResponseEntity.status(423).body(Map.of(
                        "verified", false,
                        "locked", true,
                        "attempts", attempts,
                        "message", "MPIN locked for 30 minutes after 3 failed attempts."
                ));
            }

            userRepository.save(user);
            return ResponseEntity.status(401).body(Map.of(
                    "verified", false,
                    "attemptsRemaining", 3 - attempts
            ));
        }
    }

    /**
     * Change MPIN (requires old MPIN verification first).
     */
    @PostMapping("/change")
    public ResponseEntity<Map<String, Object>> changePin(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        String oldMpin = request.get("oldMpin");
        String newMpin = request.get("newMpin");

        // Verify old PIN
        boolean valid = pinEncryptionService.verifyPin(userId, oldMpin);
        if (!valid) {
            return ResponseEntity.status(401).body(Map.of(
                    "changed", false,
                    "message", "Current MPIN is incorrect."
            ));
        }

        // Validate new PIN format
        if (newMpin == null || !newMpin.matches("\\d{4,6}")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "changed", false,
                    "message", "New MPIN must be 4-6 digits."
            ));
        }

        // Set new PIN
        pinEncryptionService.setPin(userId, newMpin);

        return ResponseEntity.ok(Map.of("changed", true, "message", "MPIN changed successfully."));
    }

    /**
     * Reset MPIN (via OTP verification - assumes OTP already verified).
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetPin(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        String newMpin = request.get("newMpin");

        if (newMpin == null || !newMpin.matches("\\d{4,6}")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "reset", false,
                    "message", "New MPIN must be 4-6 digits."
            ));
        }

        pinEncryptionService.setPin(userId, newMpin);

        // Unlock if locked
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setMpinWrongAttempts(0);
            user.setMpinLockedUntil(null);
            user.setMpinSet(true);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("reset", true, "message", "MPIN reset successfully."));
    }
}
