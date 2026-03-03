package np.com.nepalupi.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.PushNotification;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.PushNotificationRepository;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Push notification service for FCM (Firebase Cloud Messaging) and APNs (Apple Push).
 * Sends real-time push notifications for transactions, collect requests, alerts, etc.
 * In production: integrates with Firebase Admin SDK and APNs client.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final PushNotificationRepository pushRepository;
    private final UserRepository userRepository;

    /**
     * Send a push notification for a completed transaction.
     */
    public PushNotification notifyTransactionComplete(UUID userId, String upiTxnId,
                                                       Long amountPaisa, String payeeVpa) {
        double amountRs = amountPaisa / 100.0;
        String title = "Payment Successful";
        String body = String.format("Rs %.2f sent to %s. Txn: %s", amountRs, payeeVpa, upiTxnId);

        Map<String, String> data = new HashMap<>();
        data.put("type", "TRANSACTION_COMPLETE");
        data.put("txnId", upiTxnId);
        data.put("amount", String.valueOf(amountPaisa));
        data.put("payeeVpa", payeeVpa);

        return sendPush(userId, title, body, data);
    }

    /**
     * Send a push notification for incoming money.
     */
    public PushNotification notifyMoneyReceived(UUID userId, Long amountPaisa, String payerVpa) {
        double amountRs = amountPaisa / 100.0;
        String title = "Money Received";
        String body = String.format("Rs %.2f received from %s via NUPI", amountRs, payerVpa);

        Map<String, String> data = new HashMap<>();
        data.put("type", "MONEY_RECEIVED");
        data.put("amount", String.valueOf(amountPaisa));
        data.put("payerVpa", payerVpa);

        return sendPush(userId, title, body, data);
    }

    /**
     * Send a push notification for collect/payment request.
     */
    public PushNotification notifyCollectRequest(UUID userId, String requesterVpa, Long amountPaisa, String remarks) {
        double amountRs = amountPaisa / 100.0;
        String title = "Payment Request";
        String body = String.format("%s is requesting Rs %.2f. %s", requesterVpa, amountRs,
                remarks != null ? "Note: " + remarks : "");

        Map<String, String> data = new HashMap<>();
        data.put("type", "COLLECT_REQUEST");
        data.put("requesterVpa", requesterVpa);
        data.put("amount", String.valueOf(amountPaisa));

        return sendPush(userId, title, body, data);
    }

    /**
     * Send a security alert push.
     */
    public PushNotification notifySecurityAlert(UUID userId, String alertType) {
        String title = "Security Alert";
        String body = "Security event detected: " + alertType + ". If not you, contact support.";

        Map<String, String> data = new HashMap<>();
        data.put("type", "SECURITY_ALERT");
        data.put("alertType", alertType);

        return sendPush(userId, title, body, data);
    }

    /**
     * Send a promotional/informational push.
     */
    public PushNotification notifyPromo(UUID userId, String title, String body) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "PROMO");
        return sendPush(userId, title, body, data);
    }

    /**
     * Register/update FCM token for a user.
     */
    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        log.info("FCM token updated: userId={}", userId);
    }

    /**
     * Generic push notification sender.
     */
    public PushNotification sendPush(UUID userId, String title, String body, Map<String, String> data) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getFcmToken() == null) {
            log.debug("Skipping push: user {} has no FCM token", userId);
            return null;
        }

        String dataPayload = data != null ? mapToJson(data) : null;

        PushNotification push = PushNotification.builder()
                .userId(userId)
                .fcmToken(user.getFcmToken())
                .title(title)
                .body(body)
                .dataPayload(dataPayload)
                .status("QUEUED")
                .build();

        pushRepository.save(push);

        // In production: send via Firebase Admin SDK / APNs
        try {
            // Simulate FCM send
            push.setStatus("SENT");
            push.setSentAt(Instant.now());
            pushRepository.save(push);
            log.info("Push sent: userId={}, title={}", userId, title);
        } catch (Exception e) {
            push.setStatus("FAILED");
            push.setErrorMessage(e.getMessage());
            pushRepository.save(push);
            log.error("Push failed: userId={}, error={}", userId, e.getMessage());
        }

        return push;
    }

    public List<PushNotification> getHistory(UUID userId) {
        return pushRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String mapToJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}
