package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import org.springframework.stereotype.Service;

/**
 * Merchant Notification Service — real-time payment alerts.
 * <p>
 * Indian UPI model:
 * - Push notifications for every payment received
 * - Audio notification ("Paisa aayo!") for small merchants who may not check screen
 * - SMS fallback for merchants without app installed
 * - Webhook for large merchants with POS integration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantNotificationService {

    private final MerchantRepository merchantRepository;

    /**
     * Send payment received notification to merchant.
     */
    public void notifyPaymentReceived(Merchant merchant, Long amountPaisa,
                                       String payerVpa, String transactionRef) {
        String amountStr = String.format("Rs %.2f", amountPaisa / 100.0);

        // Push notification
        if (merchant.getPushEnabled()) {
            sendPushNotification(merchant, "Payment Received",
                    amountStr + " received from " + payerVpa);
        }

        // Audio notification for small merchants
        if (merchant.getAudioNotification()) {
            sendAudioNotification(merchant, amountPaisa);
        }

        // Webhook for large merchants
        if (merchant.getWebhookUrl() != null) {
            sendWebhook(merchant, amountPaisa, payerVpa, transactionRef);
        }

        log.info("Payment notification sent to merchant={} amount={}", merchant.getMerchantId(), amountStr);
    }

    /**
     * Send settlement notification.
     */
    public void notifySettlement(Merchant merchant, Long netAmountPaisa,
                                  String settlementRef, java.time.LocalDate date) {
        String amountStr = String.format("Rs %.2f", netAmountPaisa / 100.0);
        sendPushNotification(merchant, "Settlement Completed",
                amountStr + " settled to your bank account for " + date);

        log.info("Settlement notification sent to merchant={} amount={}",
                merchant.getMerchantId(), amountStr);
    }

    // ── Notification channels (in production: integrate with FCM, SMS gateway, etc.) ──

    private void sendPushNotification(Merchant merchant, String title, String body) {
        // In production: send via Firebase Cloud Messaging (FCM)
        log.debug("PUSH → merchant={}: {} - {}", merchant.getMerchantId(), title, body);
    }

    private void sendAudioNotification(Merchant merchant, Long amountPaisa) {
        // In production: trigger text-to-speech audio on merchant's device
        // "Paisa aayo! Rs XX received"
        log.debug("AUDIO → merchant={}: Rs {}", merchant.getMerchantId(),
                String.format("%.2f", amountPaisa / 100.0));
    }

    private void sendWebhook(Merchant merchant, Long amountPaisa,
                              String payerVpa, String transactionRef) {
        // In production: POST to merchant's webhook URL
        log.debug("WEBHOOK → {} payload: amount={} payer={} ref={}",
                merchant.getWebhookUrl(), amountPaisa, payerVpa, transactionRef);
    }
}
