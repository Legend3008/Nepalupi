package np.com.nepalupi.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.EmailNotification;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.EmailNotificationRepository;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Email notification service for transactional and promotional emails.
 * Supports template-based emails for receipts, alerts, KYC reminders, etc.
 * In production: integrates with SMTP server or email API (SendGrid, SES).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final EmailNotificationRepository emailRepository;
    private final UserRepository userRepository;

    /**
     * Send a transaction receipt via email.
     */
    public EmailNotification sendTransactionReceipt(UUID userId, String upiTxnId, Long amountPaisa,
                                                     String payeeVpa, String status) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null || !Boolean.TRUE.equals(user.getEmailVerified())) {
            log.debug("Skipping email: user {} has no verified email", userId);
            return null;
        }

        double amountRs = amountPaisa / 100.0;
        String subject = "NUPI Transaction " + status + " - Rs " + String.format("%.2f", amountRs);
        String body = buildTransactionReceiptBody(upiTxnId, amountRs, payeeVpa, status, user.getFullName());

        return sendEmail(userId, user.getEmail(), subject, body, "TRANSACTION_RECEIPT");
    }

    /**
     * Send a security alert email (e.g., new device login, MPIN change).
     */
    public EmailNotification sendSecurityAlert(UUID userId, String alertType, String details) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) return null;

        String subject = "NUPI Security Alert: " + alertType;
        String body = "Dear " + user.getFullName() + ",\n\n"
                + "A security event has been detected on your NUPI account:\n\n"
                + "Event: " + alertType + "\n"
                + "Details: " + details + "\n"
                + "Time: " + Instant.now() + "\n\n"
                + "If this was not you, please contact NUPI support immediately.\n\n"
                + "– NUPI Team";

        return sendEmail(userId, user.getEmail(), subject, body, "SECURITY_ALERT");
    }

    /**
     * Send KYC reminder email.
     */
    public EmailNotification sendKycReminder(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) return null;

        String subject = "Complete Your NUPI KYC Verification";
        String body = "Dear " + user.getFullName() + ",\n\n"
                + "Your NUPI KYC verification is pending. Please complete it to enjoy higher transaction limits.\n\n"
                + "Current KYC Level: " + user.getKycLevel() + "\n"
                + "Current Daily Limit: Rs " + (user.getDailyLimitPaisa() / 100.0) + "\n\n"
                + "Visit your nearest bank branch or complete eKYC in the app.\n\n"
                + "– NUPI Team";

        return sendEmail(userId, user.getEmail(), subject, body, "KYC_REMINDER");
    }

    /**
     * Generic send email method.
     */
    public EmailNotification sendEmail(UUID userId, String toEmail, String subject, String body, String templateName) {
        EmailNotification notification = EmailNotification.builder()
                .userId(userId)
                .toEmail(toEmail)
                .subject(subject)
                .body(body)
                .templateName(templateName)
                .status("QUEUED")
                .build();

        emailRepository.save(notification);

        // In production: send via SMTP/API asynchronously
        // For now, simulate immediate send
        try {
            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
            emailRepository.save(notification);
            log.info("Email sent: userId={}, to={}, template={}", userId, toEmail, templateName);
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            emailRepository.save(notification);
            log.error("Email failed: userId={}, error={}", userId, e.getMessage());
        }

        return notification;
    }

    /**
     * Get email history for a user.
     */
    public List<EmailNotification> getHistory(UUID userId) {
        return emailRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String buildTransactionReceiptBody(String upiTxnId, double amountRs, String payeeVpa,
                                                String status, String userName) {
        return "Dear " + userName + ",\n\n"
                + "Your NUPI transaction details:\n\n"
                + "Transaction ID: " + upiTxnId + "\n"
                + "Amount: Rs " + String.format("%.2f", amountRs) + "\n"
                + "Payee: " + payeeVpa + "\n"
                + "Status: " + status + "\n"
                + "Time: " + Instant.now() + "\n\n"
                + "Thank you for using NUPI.\n\n"
                + "– NUPI Team";
    }
}
