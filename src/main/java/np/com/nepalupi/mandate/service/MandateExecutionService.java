package np.com.nepalupi.mandate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.mandate.entity.Mandate;
import np.com.nepalupi.mandate.entity.MandateExecution;
import np.com.nepalupi.mandate.enums.MandateExecutionStatus;
import np.com.nepalupi.mandate.enums.MandateFrequency;
import np.com.nepalupi.mandate.enums.MandateStatus;
import np.com.nepalupi.mandate.repository.MandateExecutionRepository;
import np.com.nepalupi.mandate.repository.MandateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Mandate Execution Service — handles the actual periodic debits.
 * <p>
 * Indian UPI Autopay execution model:
 * <p>
 * 1. PRE-NOTIFICATION (D-1): 24 hours before debit, send notification to payer
 *    - Payer can still cancel or modify
 * 2. EXECUTION (D-day): At scheduled time, initiate debit
 *    - If mandate has fixed amount → debit exact amount
 *    - If variable → debit up to max_amount
 * 3. RETRY: If debit fails → retry up to 3 times over 3 days
 * 4. ADVANCE NEXT: After successful debit, calculate next debit date
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MandateExecutionService {

    private static final int MAX_RETRIES = 3;

    private final MandateExecutionRepository executionRepository;
    private final MandateRepository mandateRepository;
    private final MandateService mandateService;

    /**
     * Pre-notification job — runs daily at 8 AM, sends notifications for tomorrow's debits.
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendPreNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Mandate> dueTomorrow = mandateRepository.findDueForPreNotification(tomorrow);

        for (Mandate mandate : dueTomorrow) {
            if (mandate.getStatus() != MandateStatus.ACTIVE) continue;

            // Check cooling period for one-time mandates
            if ("ONE_TIME".equals(mandate.getMandateType()) &&
                mandate.getCoolingEndsAt() != null &&
                Instant.now().isBefore(mandate.getCoolingEndsAt())) {
                log.info("Skipping pre-notification for one-time mandate in cooling: ref={}",
                        mandate.getMandateRef());
                continue;
            }

            Long debitAmount = mandate.getAmountPaisa() != null
                    ? mandate.getAmountPaisa()
                    : mandate.getMaxAmountPaisa();

            MandateExecution execution = MandateExecution.builder()
                    .mandateId(mandate.getId())
                    .scheduledDate(tomorrow)
                    .amountPaisa(debitAmount)
                    .status(MandateExecutionStatus.PRE_NOTIFIED)
                    .preNotificationSentAt(Instant.now())
                    .build();

            executionRepository.save(execution);

            // In production: send push notification to payer
            log.info("Pre-notification sent for mandate ref={} amount={} date={}",
                    mandate.getMandateRef(), debitAmount, tomorrow);
        }

        if (!dueTomorrow.isEmpty()) {
            log.info("Sent {} pre-debit notifications for {}", dueTomorrow.size(), tomorrow);
        }
    }

    /**
     * Execution job — runs daily at 6 AM, executes today's debits.
     */
    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void executeScheduledDebits() {
        LocalDate today = LocalDate.now();
        List<MandateExecution> scheduled = executionRepository
                .findByScheduledDateAndStatus(today, MandateExecutionStatus.PRE_NOTIFIED);

        int success = 0, failed = 0;

        for (MandateExecution execution : scheduled) {
            try {
                executeDebit(execution);
                success++;
            } catch (Exception e) {
                handleFailure(execution, e.getMessage());
                failed++;
            }
        }

        log.info("Mandate execution completed for {}: {} success, {} failed",
                today, success, failed);
    }

    /**
     * Retry job — runs daily at 10 AM, retries failed executions.
     */
    @Scheduled(cron = "0 0 10 * * ?")
    @Transactional
    public void retryFailedExecutions() {
        List<MandateExecution> retryable = executionRepository
                .findByStatusIn(List.of(MandateExecutionStatus.FAILED, MandateExecutionStatus.RETRYING));

        for (MandateExecution execution : retryable) {
            if (execution.getRetryCount() >= MAX_RETRIES) {
                execution.setStatus(MandateExecutionStatus.FAILED);
                execution.setFailureReason("Max retries exceeded");
                executionRepository.save(execution);
                continue;
            }

            try {
                execution.setStatus(MandateExecutionStatus.RETRYING);
                execution.setRetryCount(execution.getRetryCount() + 1);
                executionRepository.save(execution);

                executeDebit(execution);
                log.info("Retry #{} succeeded for mandate execution={}",
                        execution.getRetryCount(), execution.getId());
            } catch (Exception e) {
                handleFailure(execution, "Retry #" + execution.getRetryCount() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Get execution history for a mandate.
     */
    public List<MandateExecution> getExecutionHistory(java.util.UUID mandateId) {
        return executionRepository.findByMandateIdOrderByScheduledDateDesc(mandateId);
    }

    // ── Internal ──

    private void executeDebit(MandateExecution execution) {
        execution.setStatus(MandateExecutionStatus.EXECUTING);
        executionRepository.save(execution);

        Mandate mandate = mandateService.getMandate(execution.getMandateId());

        // Validate amount within ceiling
        if (execution.getAmountPaisa() > mandate.getMaxAmountPaisa()) {
            throw new IllegalStateException("Execution amount exceeds mandate ceiling");
        }

        // In production:
        // 1. Create transaction via TransactionOrchestrator
        // 2. Debit payer's bank account
        // 3. Credit merchant's bank account
        // UUID txnId = transactionOrchestrator.executeMandateDebit(mandate, execution);
        // execution.setTransactionId(txnId);

        execution.setStatus(MandateExecutionStatus.COMPLETED);
        execution.setExecutedAt(Instant.now());
        executionRepository.save(execution);

        // Advance next debit date
        mandate.setLastDebitDate(execution.getScheduledDate());
        LocalDate nextDate = mandateService.calculateNextDebitDate(
                execution.getScheduledDate(), mandate.getFrequency());

        if (nextDate != null && (mandate.getEndDate() == null || !nextDate.isAfter(mandate.getEndDate()))) {
            mandate.setNextDebitDate(nextDate);
        } else {
            mandate.setStatus(MandateStatus.COMPLETED);
            mandate.setNextDebitDate(null);
        }
        mandateRepository.save(mandate);

        log.info("Mandate debit executed: ref={} amount={} date={}",
                mandate.getMandateRef(), execution.getAmountPaisa(), execution.getScheduledDate());
    }

    private void handleFailure(MandateExecution execution, String reason) {
        execution.setStatus(MandateExecutionStatus.FAILED);
        execution.setFailureReason(reason);
        executionRepository.save(execution);
        log.error("Mandate debit failed: execution={} reason={}", execution.getId(), reason);
    }
}
