package np.com.nepalupi.mandate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.mandate.entity.Mandate;
import np.com.nepalupi.mandate.entity.MandateNotification;
import np.com.nepalupi.mandate.repository.MandateNotificationRepository;
import np.com.nepalupi.mandate.repository.MandateRepository;
import np.com.nepalupi.registration.service.SmsGatewayService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Section 7.2.1: Mandate pre-debit notification service.
 * <p>
 * Sends notifications 24 hours before scheduled mandate debit.
 * Users can modify/cancel mandate before execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MandateNotificationService {

    private final MandateRepository mandateRepository;
    private final MandateNotificationRepository notificationRepository;
    private final SmsGatewayService smsGatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send pre-debit notifications 24h before execution.
     * Runs daily at 8 AM Nepal time.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendPreDebitNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Mandate> dueMandate = mandateRepository.findDueForPreNotification(tomorrow);

        int sent = 0;
        for (Mandate mandate : dueMandate) {
            try {
                sendPreDebitNotification(mandate, tomorrow);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send pre-debit notification for mandate={}: {}",
                        mandate.getMandateRef(), e.getMessage());
            }
        }

        log.info("Pre-debit notifications sent: {}/{}", sent, dueMandate.size());
    }

    /**
     * Send notification for mandate execution result.
     */
    @Transactional
    public void sendExecutionNotification(UUID mandateId, String status, Long amountPaisa) {
        MandateNotification notification = MandateNotification.builder()
                .mandateId(mandateId)
                .notificationType("EXECUTION")
                .channel("PUSH")
                .status("SENT")
                .message(String.format("Mandate execution %s. Amount: NPR %.2f",
                        status, amountPaisa / 100.0))
                .build();
        notificationRepository.save(notification);

        // Publish to Kafka for push notification delivery
        kafkaTemplate.send("notification-events", Map.of(
                "type", "MANDATE_EXECUTION",
                "mandateId", mandateId.toString(),
                "status", status,
                "amount", amountPaisa
        ));
    }

    /**
     * Send mandate expiry notification.
     */
    @Transactional
    public void sendExpiryNotification(UUID mandateId) {
        MandateNotification notification = MandateNotification.builder()
                .mandateId(mandateId)
                .notificationType("EXPIRY")
                .channel("PUSH")
                .status("SENT")
                .message("Your mandate has expired and will no longer auto-debit.")
                .build();
        notificationRepository.save(notification);
    }

    private void sendPreDebitNotification(Mandate mandate, LocalDate executionDate) {
        Long amount = mandate.getAmountPaisa() != null
                ? mandate.getAmountPaisa()
                : mandate.getMaxAmountPaisa();

        String message = String.format(
                "Nepal UPI: Auto-debit of NPR %.2f scheduled for %s from your account. " +
                "Mandate ref: %s. To cancel, open your Nepal UPI app before %s.",
                amount / 100.0, executionDate, mandate.getMandateRef(), executionDate);

        MandateNotification notification = MandateNotification.builder()
                .mandateId(mandate.getId())
                .notificationType("PRE_DEBIT")
                .channel("PUSH")
                .status("SENT")
                .message(message)
                .build();
        notificationRepository.save(notification);

        // Publish to notification topic
        kafkaTemplate.send("notification-events", Map.of(
                "type", "MANDATE_PRE_DEBIT",
                "mandateId", mandate.getId().toString(),
                "mandateRef", mandate.getMandateRef(),
                "payerVpa", mandate.getPayerVpa(),
                "amount", amount,
                "executionDate", executionDate.toString()
        ));

        log.info("Pre-debit notification sent: mandate={}, amount={}, date={}",
                mandate.getMandateRef(), amount, executionDate);
    }
}
