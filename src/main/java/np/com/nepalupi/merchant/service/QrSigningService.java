package np.com.nepalupi.merchant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Section 19 (Signed QR): QR Code Digital Signature Service.
 * <p>
 * Provides digital signing for QR codes to prevent tampering:
 * <ul>
 *   <li>HMAC-SHA256 for static QR signatures (fast, symmetric)</li>
 *   <li>ECDSA P-256 for dynamic QR signatures (asymmetric, verifiable by PSPs)</li>
 *   <li>Signature embedded in QR payload as 'sign' parameter</li>
 *   <li>Timestamp included to prevent replay attacks</li>
 *   <li>Key rotation support</li>
 * </ul>
 * 
 * Signed QR payload example:
 * upi://pay?pa=merchant@nchl&am=100.00&cu=NPR&ts=1704067200&sign=base64signature
 */
@Service
@Slf4j
public class QrSigningService {

    @Value("${nepalupi.qr.signing.hmac-key:nepal-upi-qr-signing-key-2024}")
    private String hmacSigningKey;

    @Value("${nepalupi.qr.signing.algorithm:HMAC}")
    private String signingAlgorithm; // HMAC or ECDSA

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String EC_ALGORITHM = "SHA256withECDSA";

    // ECDSA keys (loaded from config in production)
    private KeyPair ecKeyPair;

    /**
     * Sign a QR payload and return the signed version.
     *
     * @param qrPayload the original QR payload (upi://pay?...)
     * @return signed payload with timestamp and signature appended
     */
    public String signQrPayload(String qrPayload) {
        long timestamp = Instant.now().getEpochSecond();
        String dataToSign = qrPayload + "&ts=" + timestamp;

        String signature;
        if ("ECDSA".equalsIgnoreCase(signingAlgorithm)) {
            signature = signWithEcdsa(dataToSign);
        } else {
            signature = signWithHmac(dataToSign);
        }

        String signedPayload = dataToSign + "&sign=" + signature;
        log.debug("QR payload signed: algorithm={}, ts={}", signingAlgorithm, timestamp);
        return signedPayload;
    }

    /**
     * Verify a signed QR payload.
     *
     * @param signedPayload complete signed payload
     * @return verification result
     */
    public QrVerificationResult verifySignedQr(String signedPayload) {
        try {
            // Extract signature
            int signIndex = signedPayload.lastIndexOf("&sign=");
            if (signIndex < 0) {
                return new QrVerificationResult(false, "No signature found", null);
            }

            String signature = signedPayload.substring(signIndex + 6);
            String dataToVerify = signedPayload.substring(0, signIndex);

            // Extract timestamp
            int tsIndex = dataToVerify.lastIndexOf("&ts=");
            if (tsIndex < 0) {
                return new QrVerificationResult(false, "No timestamp found", null);
            }

            long timestamp = Long.parseLong(dataToVerify.substring(tsIndex + 4));
            Instant signedAt = Instant.ofEpochSecond(timestamp);

            // Check if signature is too old (15 min for dynamic QR)
            if (signedAt.plusSeconds(900).isBefore(Instant.now())) {
                return new QrVerificationResult(false, "Signature expired", signedAt);
            }

            // Verify signature
            boolean valid;
            if ("ECDSA".equalsIgnoreCase(signingAlgorithm)) {
                valid = verifyEcdsa(dataToVerify, signature);
            } else {
                valid = verifyHmac(dataToVerify, signature);
            }

            if (valid) {
                return new QrVerificationResult(true, "Signature valid", signedAt);
            } else {
                return new QrVerificationResult(false, "Invalid signature", signedAt);
            }
        } catch (Exception e) {
            log.error("QR signature verification error: {}", e.getMessage());
            return new QrVerificationResult(false, "Verification error: " + e.getMessage(), null);
        }
    }

    /**
     * Strip signature from a signed QR payload to get the original.
     */
    public String stripSignature(String signedPayload) {
        int tsIndex = signedPayload.lastIndexOf("&ts=");
        if (tsIndex > 0) {
            return signedPayload.substring(0, tsIndex);
        }
        return signedPayload;
    }

    // ── HMAC Signing ──

    private String signWithHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    hmacSigningKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC QR signing failed", e);
        }
    }

    private boolean verifyHmac(String data, String expectedSignature) {
        String computed = signWithHmac(data);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8));
    }

    // ── ECDSA Signing ──

    private String signWithEcdsa(String data) {
        try {
            ensureEcKeyPair();
            Signature signer = Signature.getInstance(EC_ALGORITHM);
            signer.initSign(ecKeyPair.getPrivate());
            signer.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signer.sign();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("ECDSA QR signing failed", e);
        }
    }

    private boolean verifyEcdsa(String data, String signature) {
        try {
            ensureEcKeyPair();
            Signature verifier = Signature.getInstance(EC_ALGORITHM);
            verifier.initVerify(ecKeyPair.getPublic());
            verifier.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getUrlDecoder().decode(signature);
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            log.error("ECDSA verification error: {}", e.getMessage());
            return false;
        }
    }

    private synchronized void ensureEcKeyPair() {
        if (ecKeyPair == null) {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
                keyGen.initialize(256, new SecureRandom());
                ecKeyPair = keyGen.generateKeyPair();
                log.info("ECDSA key pair generated for QR signing");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to generate ECDSA key pair", e);
            }
        }
    }

    /**
     * Rotate the ECDSA key pair (for key rotation schedules).
     */
    public void rotateSigningKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(256, new SecureRandom());
            ecKeyPair = keyGen.generateKeyPair();
            log.info("ECDSA signing key pair rotated");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Key rotation failed", e);
        }
    }

    /**
     * Get the public key for PSPs to verify QR signatures.
     */
    public String getPublicKeyBase64() {
        ensureEcKeyPair();
        return Base64.getEncoder().encodeToString(ecKeyPair.getPublic().getEncoded());
    }

    // --- Record ---

    public record QrVerificationResult(boolean valid, String message, Instant signedAt) {}
}
