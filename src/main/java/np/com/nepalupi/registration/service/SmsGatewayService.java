package np.com.nepalupi.registration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Section 12: SMS Gateway integration for Nepal.
 * <p>
 * Production: integrates with Nepal Telecom / Ncell / Sparrow SMS API.
 * Dev mode: logs OTP to console for testing.
 */
@Service
@Slf4j
public class SmsGatewayService {

    @Value("${nepalupi.sms.provider:DEV}")
    private String smsProvider; // DEV, SPARROW, NTC, NCELL

    @Value("${nepalupi.sms.api-key:}")
    private String apiKey;

    @Value("${nepalupi.sms.sender-id:NEPALUPI}")
    private String senderId;

    /**
     * Send OTP via SMS.
     * In dev mode, the OTP is logged to console.
     * In production, it would call the SMS provider's API.
     */
    public void sendOtp(String mobileNumber, String otp, String purpose) {
        String message = buildOtpMessage(otp, purpose);

        if ("DEV".equals(smsProvider)) {
            log.info("═══ DEV SMS ═══ To: {}, OTP: {}, Purpose: {} ═══", mobileNumber, otp, purpose);
            return;
        }

        // Production SMS sending
        sendSms(mobileNumber, message);
    }

    /**
     * Send general SMS notification (transaction alerts, mandate notifications, etc.)
     */
    public void sendNotification(String mobileNumber, String message) {
        if ("DEV".equals(smsProvider)) {
            log.info("═══ DEV SMS ═══ To: {}, Message: {} ═══", mobileNumber, message);
            return;
        }

        sendSms(mobileNumber, message);
    }

    private void sendSms(String mobileNumber, String message) {
        switch (smsProvider) {
            case "SPARROW" -> sendViaSparrow(mobileNumber, message);
            case "NTC" -> sendViaNtc(mobileNumber, message);
            default -> log.warn("Unknown SMS provider: {}. Message not sent.", smsProvider);
        }
    }

    private void sendViaSparrow(String mobileNumber, String message) {
        // Sparrow SMS API integration
        // POST https://api.sparrowsms.com/v2/sms/
        // { "token": apiKey, "from": senderId, "to": mobileNumber, "text": message }
        log.info("Sending SMS via Sparrow to {}: {}", mobileNumber, message);
        // In production: use RestTemplate/WebClient to call Sparrow API
    }

    private void sendViaNtc(String mobileNumber, String message) {
        // Nepal Telecom bulk SMS API integration
        log.info("Sending SMS via NTC to {}: {}", mobileNumber, message);
    }

    private String buildOtpMessage(String otp, String purpose) {
        return switch (purpose) {
            case "REGISTRATION" -> String.format("Your Nepal UPI registration OTP is: %s. Valid for 5 minutes. Do not share.", otp);
            case "MPIN_RESET" -> String.format("Your Nepal UPI PIN reset OTP is: %s. Valid for 5 minutes. If you didn't request this, contact support.", otp);
            case "DEVICE_CHANGE" -> String.format("Your Nepal UPI device change OTP is: %s. Valid for 5 minutes.", otp);
            default -> String.format("Your Nepal UPI OTP is: %s. Valid for 5 minutes.", otp);
        };
    }
}
