package np.com.nepalupi.service.pin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles UPI MPIN encryption and PIN block formatting.
 * <p>
 * <strong>PRODUCTION NOTE:</strong> In production, this MUST use a Hardware Security Module
 * (HSM — Thales Luna or AWS CloudHSM). Software-based PIN encryption will NOT pass
 * NRB audit. This implementation is a development placeholder only.
 * <p>
 * Flow:
 * <pre>
 * User enters MPIN → Encrypted with Bank's Public Key (via HSM) → Sent to Bank
 * Only the bank can decrypt the PIN (with its private key in their HSM)
 * </pre>
 */
@Service
@Slf4j
public class PinEncryptionService {

    // In production: injected HSM client (e.g., HsmClient hsmClient)
    // For dev: in-memory RSA key pairs per bank
    private final Map<String, KeyPair> bankKeys = new ConcurrentHashMap<>();

    // Dev placeholder: In production, hashed PINs stored in secure DB / HSM
    private final Map<UUID, String> userPinHashes = new ConcurrentHashMap<>();

    /**
     * Set/update a user's MPIN.
     * PRODUCTION NOTE: PIN should be hashed with PBKDF2/bcrypt and stored in DB.
     *
     * @param userId the user ID
     * @param pin    the 4-6 digit MPIN
     */
    public void setPin(UUID userId, String pin) {
        String hash = hashPin(pin);
        userPinHashes.put(userId, hash);
        log.info("MPIN set for user: {}", userId);
    }

    /**
     * Verify a user's MPIN.
     *
     * @param userId the user ID
     * @param pin    the PIN to verify
     * @return true if PIN matches
     */
    public boolean verifyPin(UUID userId, String pin) {
        String storedHash = userPinHashes.get(userId);
        if (storedHash == null) {
            log.warn("No MPIN set for user: {}", userId);
            return false;
        }
        return storedHash.equals(hashPin(pin));
    }

    private String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("PIN hashing failed", e);
        }
    }

    /**
     * Encrypt a user's MPIN with the target bank's public key.
     * The encrypted PIN is sent alongside the debit request to the bank.
     * Only the bank can decrypt it.
     *
     * @param pin           the user's 4-6 digit MPIN
     * @param bankCode      the bank whose public key to use
     * @return Base64-encoded encrypted PIN
     */
    public String encryptPin(String pin, String bankCode) {
        log.debug("Encrypting PIN for bank: {}", bankCode);

        // PRODUCTION: return hsmClient.encrypt(pin, bankPublicKeyId, RSA_2048);
        try {
            PublicKey publicKey = getOrCreateBankPublicKey(bankCode);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("PIN encryption failed for bank {}", bankCode, e);
            throw new RuntimeException("PIN encryption failed", e);
        }
    }

    /**
     * Format PIN block in ISO Format 0 (same as India UPI).
     *
     * @param pin the user's MPIN
     * @param pan Primary Account Number (last 12 digits of account excluding check digit)
     * @return formatted PIN block as hex string
     */
    public String formatPinBlock(String pin, String pan) {
        // PRODUCTION: return hsmClient.formatPinBlock(pin, pan, ISO_FORMAT_0);

        // ISO Format 0: XOR of PIN field with PAN field
        // PIN field:  0 + PIN length + PIN + pad with F
        // PAN field:  0000 + rightmost 12 digits of PAN (excluding check digit)

        String pinField = String.format("0%d%s", pin.length(), pin);
        while (pinField.length() < 16) {
            pinField += "F";
        }

        // Extract rightmost 12 digits of PAN (excluding last check digit)
        String panRight = pan.length() > 13
                ? pan.substring(pan.length() - 13, pan.length() - 1)
                : pan.substring(0, Math.min(pan.length() - 1, 12));
        String panField = "0000" + panRight;
        while (panField.length() < 16) {
            panField = "0" + panField;
        }

        // XOR
        long pinLong = Long.parseUnsignedLong(pinField, 16);
        long panLong = Long.parseUnsignedLong(panField, 16);
        long result = pinLong ^ panLong;
        return String.format("%016X", result);
    }

    private PublicKey getOrCreateBankPublicKey(String bankCode) throws Exception {
        KeyPair kp = bankKeys.computeIfAbsent(bankCode, k -> {
            try {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048);
                return gen.generateKeyPair();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate key pair for bank " + k, e);
            }
        });
        return kp.getPublic();
    }
}
