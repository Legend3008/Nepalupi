package np.com.nepalupi.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.domain.event.TransactionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Kafka consumer that handles transaction events and triggers push notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    // In production: inject FCM/APNs push notification service
    // private final PushNotificationService pushService;

    @KafkaListener(topics = "${nepalupi.kafka.topics.transaction-events:transaction-events}",
                   groupId = "notification-service")
    public void handleTransactionEvent(TransactionEvent event) {
        log.info("Notification consumer received event: txn={}, status={}",
                event.getUpiTxnId(), event.getStatus());

        if (event.getStatus() == TransactionStatus.COMPLETED) {
            String formattedAmount = formatAmount(event.getAmount());

            // Notify payer
            String payerMsg = String.format("रू %s sent to %s", formattedAmount, event.getPayeeVpa());
            sendPush(event.getPayerUserId() != null ? event.getPayerUserId().toString() : event.getPayerVpa(),
                    payerMsg);

            // Notify payee
            String payeeMsg = String.format("रू %s received from %s", formattedAmount, event.getPayerVpa());
            sendPush(event.getPayeeUserId() != null ? event.getPayeeUserId().toString() : event.getPayeeVpa(),
                    payeeMsg);

        } else if (event.getStatus() == TransactionStatus.DEBIT_FAILED) {
            String payerMsg = String.format("Payment of रू %s to %s failed: %s",
                    formatAmount(event.getAmount()), event.getPayeeVpa(),
                    event.getFailureCode() != null ? event.getFailureCode() : "Unknown error");
            sendPush(event.getPayerVpa(), payerMsg);

        } else if (event.getStatus() == TransactionStatus.REVERSED) {
            String payerMsg = String.format("रू %s refunded for failed payment to %s",
                    formatAmount(event.getAmount()), event.getPayeeVpa());
            sendPush(event.getPayerVpa(), payerMsg);
        }
    }

    /**
     * Format paisa to NPR display string.
     * 150050 paisa → "1,500.50"
     */
    private String formatAmount(Long paisa) {
        if (paisa == null) return "0.00";
        double rupees = paisa / 100.0;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "NP"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(rupees);
    }

    private void sendPush(String target, String message) {
        // In production: pushService.send(target, message);
        log.info("[PUSH NOTIFICATION] To: {} | Message: {}", target, message);
    }
}
