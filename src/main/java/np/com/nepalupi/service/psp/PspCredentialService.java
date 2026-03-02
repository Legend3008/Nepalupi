package np.com.nepalupi.service.psp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Psp;
import np.com.nepalupi.repository.PspRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Manages PSP credentials: API keys, sandbox tokens, webhook signing secrets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspCredentialService {

    private final PspRepository pspRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * Generate a sandbox token for pilot testing.
     */
    public String generateSandboxToken(UUID pspId) {
        String token = "sbx_" + generateSecureToken(32);
        log.info("Sandbox token generated for PSP {}", pspId);
        return token;
    }

    /**
     * Generate production API key + secret and persist hashes.
     * Returns the raw credentials (shown only once).
     */
    @Transactional
    public ProductionCredentials generateProductionCredentials(Psp psp) {
        String apiKey = "nupi_" + generateSecureToken(32);
        String secret = "nups_" + generateSecureToken(48);
        String webhookSecret = "whsec_" + generateSecureToken(32);

        // Store BCrypt hashed values
        psp.setApiKeyHash(hashCredential(apiKey));
        psp.setSecretHash(hashCredential(secret));
        psp.setWebhookSigningSecret(webhookSecret);

        pspRepository.save(psp);
        log.info("Production credentials generated for PSP {}", psp.getPspId());

        return new ProductionCredentials(apiKey, secret, webhookSecret);
    }

    /**
     * Rotate API key for an existing PSP.
     */
    @Transactional
    public ProductionCredentials rotateApiKey(UUID pspId) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        String newApiKey = "nupi_" + generateSecureToken(32);
        String newSecret = "nups_" + generateSecureToken(48);

        psp.setApiKeyHash(hashCredential(newApiKey));
        psp.setSecretHash(hashCredential(newSecret));
        pspRepository.save(psp);

        log.info("API key rotated for PSP {}", psp.getPspId());
        return new ProductionCredentials(newApiKey, newSecret, psp.getWebhookSigningSecret());
    }

    /**
     * Rotate webhook signing secret.
     */
    @Transactional
    public String rotateWebhookSecret(UUID pspId) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        String newSecret = "whsec_" + generateSecureToken(32);
        psp.setWebhookSigningSecret(newSecret);
        pspRepository.save(psp);

        log.info("Webhook secret rotated for PSP {}", psp.getPspId());
        return newSecret;
    }

    /**
     * Register client certificate fingerprint for mTLS.
     */
    @Transactional
    public void registerClientCertificate(UUID pspId, String certFingerprint) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        psp.setClientCertFingerprint(certFingerprint);
        pspRepository.save(psp);

        log.info("Client certificate registered for PSP {}: fingerprint={}", psp.getPspId(), certFingerprint);
    }

    // ── Helpers ──────────────────────────────────────────────

    private String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * BCrypt-hashed credential storage.
     */
    private String hashCredential(String raw) {
        return passwordEncoder.encode(raw);
    }

    /**
     * Verify a raw credential against a stored BCrypt hash.
     */
    public boolean verifyCredential(String raw, String hash) {
        return passwordEncoder.matches(raw, hash);
    }

    // ── Credential record (returned once) ────────────────────

    public record ProductionCredentials(
            String apiKey,
            String secret,
            String webhookSigningSecret
    ) {}
}
