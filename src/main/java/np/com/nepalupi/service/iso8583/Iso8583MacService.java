package np.com.nepalupi.service.iso8583;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Section 14: ISO 8583 MAC (Message Authentication Code) computation.
 * <p>
 * Field 64 (primary MAC) or Field 128 (secondary MAC) is used for message
 * integrity verification in ISO 8583 financial messages. The MAC ensures
 * that the message was not tampered with during transmission between the
 * NPI switch and NCHL/bank endpoints.
 * <p>
 * MAC Algorithms supported:
 * - HMAC-SHA256 (default for Nepal UPI)
 * - HMAC-SHA1 (legacy fallback)
 * - CBC-MAC with 3DES (for legacy NCHL integration)
 * <p>
 * The MAC is computed over selected ISO 8583 fields (typically F2, F3, F4,
 * F7, F11, F37, F41, F42, F49) using a shared session key established
 * during sign-on.
 */
@Component
@Slf4j
public class Iso8583MacService {

    @Value("${nepalupi.iso8583.mac.algorithm:HmacSHA256}")
    private String macAlgorithm;

    @Value("${nepalupi.iso8583.mac.key:NPI-DEFAULT-MAC-KEY-CHANGE-IN-PRODUCTION}")
    private String macKeyBase64;

    @Value("${nepalupi.iso8583.mac.enabled:true}")
    private boolean macEnabled;

    /**
     * Compute MAC for an ISO 8583 message (field 64).
     * The MAC is calculated over concatenated critical fields.
     *
     * @param message the ISO 8583 message
     * @return MAC as hex string (16 bytes / 32 hex chars for SHA-256)
     */
    public String computeMac(Iso8583MessageBuilder.Iso8583Message message) {
        if (!macEnabled) {
            log.debug("MAC computation disabled — returning empty MAC");
            return "";
        }

        try {
            String macInput = buildMacInput(message);
            byte[] keyBytes = macKeyBase64.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, macAlgorithm);

            Mac mac = Mac.getInstance(macAlgorithm);
            mac.init(keySpec);

            byte[] macBytes = mac.doFinal(macInput.getBytes(StandardCharsets.UTF_8));

            // Take first 8 bytes (64 bits) as per ISO 8583 field 64 specification
            String fullMac = bytesToHex(macBytes);
            String field64Mac = fullMac.substring(0, Math.min(16, fullMac.length()));

            log.debug("MAC computed for MTI={}, STAN={}: {}",
                    message.getMti(), message.getField11(), field64Mac);

            return field64Mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("MAC computation failed for STAN={}: {}", message.getField11(), e.getMessage());
            throw new RuntimeException("MAC computation failed", e);
        }
    }

    /**
     * Verify the MAC of a received ISO 8583 message.
     *
     * @param message     the received message
     * @param receivedMac the MAC from field 64/128
     * @return true if MAC is valid
     */
    public boolean verifyMac(Iso8583MessageBuilder.Iso8583Message message, String receivedMac) {
        if (!macEnabled) {
            return true;
        }

        if (receivedMac == null || receivedMac.isEmpty()) {
            log.warn("No MAC present in received message MTI={}, STAN={}",
                    message.getMti(), message.getField11());
            return false;
        }

        String computedMac = computeMac(message);
        boolean valid = computedMac.equalsIgnoreCase(receivedMac);

        if (!valid) {
            log.error("MAC VERIFICATION FAILED for MTI={}, STAN={}. Expected={}, Received={}",
                    message.getMti(), message.getField11(), computedMac, receivedMac);
        }

        return valid;
    }

    /**
     * Build the MAC input string by concatenating critical fields.
     * <p>
     * Fields included in MAC calculation (per NCHL specification):
     * MTI + F2 + F3 + F4 + F7 + F11 + F37 + F41 + F42 + F49
     */
    private String buildMacInput(Iso8583MessageBuilder.Iso8583Message msg) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, msg.getMti());
        appendField(sb, msg.getField2());
        appendField(sb, msg.getField3());
        appendField(sb, msg.getField4());
        appendField(sb, msg.getField7());
        appendField(sb, msg.getField11());
        appendField(sb, msg.getField37());
        appendField(sb, msg.getField41());
        appendField(sb, msg.getField42());
        appendField(sb, msg.getField49());
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String field) {
        if (field != null && !field.isEmpty()) {
            sb.append(field);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

    /**
     * Rotate the MAC session key.
     * Called during sign-on or key exchange.
     */
    public void rotateSessionKey(String newKeyBase64) {
        log.info("MAC session key rotated");
        this.macKeyBase64 = newKeyBase64;
    }

    /**
     * Check if MAC is enabled.
     */
    public boolean isMacEnabled() {
        return macEnabled;
    }
}
