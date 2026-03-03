package np.com.nepalupi.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Section 12.3: Biometric Authentication Support.
 * <p>
 * Framework for biometric-based transaction authorization as an 
 * alternative to MPIN for supported devices:
 * <ul>
 *   <li>Fingerprint authentication via device hardware</li>
 *   <li>Face recognition via device hardware</li>
 *   <li>Device attestation validation (Android SafetyNet / iOS DeviceCheck)</li>
 *   <li>Biometric enrollment and de-enrollment</li>
 *   <li>Fallback to MPIN if biometric fails</li>
 * </ul>
 * 
 * Note: Actual biometric matching happens on-device. The server validates
 * the signed attestation from the device's secure enclave (TEE/SE).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricAuthService {

    private final StringRedisTemplate redisTemplate;

    private static final String BIOMETRIC_KEY = "biometric:enrolled:";
    private static final String CHALLENGE_KEY = "biometric:challenge:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final int MAX_BIOMETRIC_FAILURES = 5;

    /**
     * Enroll biometric authentication for a user on a specific device.
     *
     * @param userId          user ID
     * @param deviceId        device identifier
     * @param biometricType   FINGERPRINT or FACE
     * @param publicKeyBase64 the public key from device's secure enclave
     * @return enrollment ID
     */
    public String enrollBiometric(UUID userId, String deviceId, BiometricType biometricType,
                                   String publicKeyBase64) {
        String enrollmentId = UUID.randomUUID().toString();
        String key = BIOMETRIC_KEY + userId;

        Map<String, String> enrollment = Map.of(
                "enrollmentId", enrollmentId,
                "deviceId", deviceId,
                "biometricType", biometricType.name(),
                "publicKey", publicKeyBase64,
                "enrolledAt", Instant.now().toString(),
                "status", "ACTIVE",
                "failureCount", "0"
        );

        enrollment.forEach((k, v) -> redisTemplate.opsForHash().put(key, k, v));

        log.info("Biometric enrolled: user={}, device={}, type={}, enrollmentId={}",
                userId, deviceId, biometricType, enrollmentId);
        return enrollmentId;
    }

    /**
     * Generate a challenge for biometric authentication.
     * The challenge is a random nonce that the device must sign with
     * the enrolled biometric key.
     */
    public String generateChallenge(UUID userId) {
        String challenge = UUID.randomUUID().toString() + "-" + Instant.now().toEpochMilli();
        String key = CHALLENGE_KEY + userId;
        redisTemplate.opsForValue().set(key, challenge, CHALLENGE_TTL);

        log.debug("Biometric challenge generated for user={}", userId);
        return challenge;
    }

    /**
     * Verify the biometric authentication response from the device.
     *
     * @param userId           user ID
     * @param deviceId         device making the request
     * @param signedChallenge  the challenge signed by device's secure enclave
     * @param attestationData  device attestation (SafetyNet/DeviceCheck)
     * @return true if authentication succeeded
     */
    public boolean verifyBiometric(UUID userId, String deviceId, String signedChallenge,
                                    String attestationData) {
        // 1. Validate challenge exists and hasn't expired
        String key = CHALLENGE_KEY + userId;
        String storedChallenge = redisTemplate.opsForValue().get(key);
        if (storedChallenge == null) {
            log.warn("Biometric challenge expired or not found: user={}", userId);
            return false;
        }

        // 2. Validate enrollment exists for this device
        String enrollKey = BIOMETRIC_KEY + userId;
        String enrolledDevice = (String) redisTemplate.opsForHash().get(enrollKey, "deviceId");
        if (!deviceId.equals(enrolledDevice)) {
            log.warn("Biometric device mismatch: user={}, expected={}, got={}", 
                    userId, enrolledDevice, deviceId);
            return false;
        }

        // 3. Check enrollment is active
        String status = (String) redisTemplate.opsForHash().get(enrollKey, "status");
        if (!"ACTIVE".equals(status)) {
            log.warn("Biometric enrollment not active: user={}, status={}", userId, status);
            return false;
        }

        // 4. Validate attestation data (device integrity check)
        if (!validateDeviceAttestation(attestationData)) {
            log.warn("Device attestation failed: user={}", userId);
            return false;
        }

        // 5. Verify signature against stored public key
        // In production, this would verify the signedChallenge against the public key
        // using the TEE/SE signature algorithm (e.g., ECDSA with P-256)
        String publicKey = (String) redisTemplate.opsForHash().get(enrollKey, "publicKey");
        boolean signatureValid = verifySignature(storedChallenge, signedChallenge, publicKey);

        if (!signatureValid) {
            handleFailure(userId, enrollKey);
            return false;
        }

        // 6. Success — clear challenge and reset failure count
        redisTemplate.delete(key);
        redisTemplate.opsForHash().put(enrollKey, "failureCount", "0");
        redisTemplate.opsForHash().put(enrollKey, "lastAuthAt", Instant.now().toString());

        log.info("Biometric authentication successful: user={}, device={}", userId, deviceId);
        return true;
    }

    /**
     * Check if user has biometric enrolled.
     */
    public boolean isBiometricEnrolled(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BIOMETRIC_KEY + userId));
    }

    /**
     * De-enroll biometric for a user.
     */
    public void deEnrollBiometric(UUID userId) {
        redisTemplate.delete(BIOMETRIC_KEY + userId);
        log.info("Biometric de-enrolled: user={}", userId);
    }

    /**
     * Get biometric enrollment status.
     */
    public Optional<Map<Object, Object>> getEnrollmentStatus(UUID userId) {
        String key = BIOMETRIC_KEY + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return Optional.of(redisTemplate.opsForHash().entries(key));
        }
        return Optional.empty();
    }

    private void handleFailure(UUID userId, String enrollKey) {
        String countStr = (String) redisTemplate.opsForHash().get(enrollKey, "failureCount");
        int count = countStr != null ? Integer.parseInt(countStr) + 1 : 1;
        redisTemplate.opsForHash().put(enrollKey, "failureCount", String.valueOf(count));

        if (count >= MAX_BIOMETRIC_FAILURES) {
            redisTemplate.opsForHash().put(enrollKey, "status", "LOCKED");
            log.warn("Biometric locked after {} failures: user={}", count, userId);
        }

        log.warn("Biometric verification failed: user={}, attempt={}/{}", userId, count, MAX_BIOMETRIC_FAILURES);
    }

    private boolean validateDeviceAttestation(String attestationData) {
        // In production: validate Android SafetyNet / Google Play Integrity API
        // or iOS DeviceCheck attestation
        return attestationData != null && !attestationData.isBlank();
    }

    private boolean verifySignature(String challenge, String signedChallenge, String publicKey) {
        // In production: use java.security.Signature with EC/P-256
        // to verify the signed challenge against the enrolled public key
        return signedChallenge != null && !signedChallenge.isBlank() && publicKey != null;
    }

    public enum BiometricType {
        FINGERPRINT,
        FACE
    }
}
