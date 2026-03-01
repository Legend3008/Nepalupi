package np.com.nepalupi.mandate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.mandate.entity.Mandate;
import np.com.nepalupi.mandate.enums.MandateFrequency;
import np.com.nepalupi.mandate.enums.MandateStatus;
import np.com.nepalupi.mandate.repository.MandateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Mandate Service — UPI Autopay / Recurring payments.
 * <p>
 * Indian UPI model (exactly replicated):
 * <p>
 * 1. Merchant/biller creates mandate → payer receives approval request
 * 2. Payer reviews terms (amount, frequency, max amount) → approves with MPIN
 * 3. Mandate becomes ACTIVE → scheduled debits execute automatically
 * 4. Pre-debit notification sent 24h before each debit
 * 5. Payer can pause/cancel mandate anytime
 * <p>
 * One-time mandate (for large payments):
 * - Pre-authorized single debit at a future date
 * - 12-hour cooling period where payer can revoke
 * <p>
 * Categories: SUBSCRIPTION, LOAN_EMI, INSURANCE, UTILITY, MUTUAL_FUND
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MandateService {

    private final MandateRepository mandateRepository;

    /**
     * Create a new mandate (merchant initiates, payer must approve).
     */
    @Transactional
    public Mandate createMandate(String merchantVpa, String payerVpa,
                                  Long amountPaisa, Long maxAmountPaisa,
                                  MandateFrequency frequency, String category,
                                  String purpose, LocalDate startDate, LocalDate endDate,
                                  boolean isOneTime) {
        log.info("Creating {} mandate: {} → {} max={} freq={}",
                isOneTime ? "one-time" : "recurring", merchantVpa, payerVpa,
                maxAmountPaisa, frequency);

        String mandateRef = generateMandateRef();

        Mandate mandate = Mandate.builder()
                .mandateRef(mandateRef)
                .merchantVpa(merchantVpa)
                .payerVpa(payerVpa)
                .amountPaisa(amountPaisa)
                .maxAmountPaisa(maxAmountPaisa)
                .frequency(frequency)
                .category(np.com.nepalupi.mandate.enums.MandateCategory.valueOf(category))
                .mandateType(isOneTime ? "ONE_TIME" : "RECURRING")
                .purpose(purpose)
                .startDate(startDate)
                .endDate(endDate)
                .nextDebitDate(startDate)
                .status(MandateStatus.PENDING_APPROVAL)
                .coolingPeriodMinutes(isOneTime ? 720 : 0) // 12h cooling for one-time
                .build();

        mandate = mandateRepository.save(mandate);

        // In production: send approval notification to payer's PSP app
        log.info("Mandate created: ref={} {} type={}", mandateRef, payerVpa,
                mandate.getMandateType());
        return mandate;
    }

    /**
     * Approve a mandate — payer confirms with MPIN.
     */
    @Transactional
    public Mandate approve(UUID mandateId) {
        Mandate mandate = getMandate(mandateId);

        if (mandate.getStatus() != MandateStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Mandate not in PENDING_APPROVAL state");
        }

        mandate.setStatus(MandateStatus.ACTIVE);
        mandate.setApprovedAt(Instant.now());

        // One-time mandates get a cooling period
        if ("ONE_TIME".equals(mandate.getMandateType()) && mandate.getCoolingPeriodMinutes() > 0) {
            mandate.setCoolingEndsAt(
                    Instant.now().plusSeconds(mandate.getCoolingPeriodMinutes() * 60L));
            log.info("Mandate approved with cooling period until: {}", mandate.getCoolingEndsAt());
        }

        mandate = mandateRepository.save(mandate);
        log.info("Mandate approved: ref={}", mandate.getMandateRef());
        return mandate;
    }

    /**
     * Pause a mandate — payer temporarily stops debits.
     */
    @Transactional
    public Mandate pause(UUID mandateId) {
        Mandate mandate = getMandate(mandateId);
        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            throw new IllegalStateException("Can only pause ACTIVE mandates");
        }
        mandate.setStatus(MandateStatus.PAUSED);
        mandate.setPausedAt(Instant.now());
        mandate = mandateRepository.save(mandate);
        log.info("Mandate paused: ref={}", mandate.getMandateRef());
        return mandate;
    }

    /**
     * Resume a paused mandate.
     */
    @Transactional
    public Mandate resume(UUID mandateId) {
        Mandate mandate = getMandate(mandateId);
        if (mandate.getStatus() != MandateStatus.PAUSED) {
            throw new IllegalStateException("Can only resume PAUSED mandates");
        }
        mandate.setStatus(MandateStatus.ACTIVE);
        mandate.setPausedAt(null);
        mandate = mandateRepository.save(mandate);
        log.info("Mandate resumed: ref={}", mandate.getMandateRef());
        return mandate;
    }

    /**
     * Cancel a mandate — permanent cancellation by payer.
     */
    @Transactional
    public Mandate cancel(UUID mandateId, String reason) {
        Mandate mandate = getMandate(mandateId);
        if (mandate.getStatus() == MandateStatus.CANCELLED ||
            mandate.getStatus() == MandateStatus.EXPIRED) {
            throw new IllegalStateException("Mandate already cancelled/expired");
        }
        mandate.setStatus(MandateStatus.CANCELLED);
        mandate.setCancelledAt(Instant.now());
        mandate.setCancellationReason(reason);
        mandate = mandateRepository.save(mandate);
        log.info("Mandate cancelled: ref={} reason={}", mandate.getMandateRef(), reason);
        return mandate;
    }

    /**
     * Revoke a one-time mandate during cooling period.
     */
    @Transactional
    public Mandate revokeDuringCooling(UUID mandateId) {
        Mandate mandate = getMandate(mandateId);
        if (!"ONE_TIME".equals(mandate.getMandateType())) {
            throw new IllegalStateException("Only one-time mandates can be revoked during cooling");
        }
        if (mandate.getCoolingEndsAt() == null || Instant.now().isAfter(mandate.getCoolingEndsAt())) {
            throw new IllegalStateException("Cooling period has ended — cannot revoke");
        }
        mandate.setStatus(MandateStatus.CANCELLED);
        mandate.setCancelledAt(Instant.now());
        mandate.setCancellationReason("Revoked during cooling period");
        mandate = mandateRepository.save(mandate);
        log.info("One-time mandate revoked during cooling: ref={}", mandate.getMandateRef());
        return mandate;
    }

    /**
     * Get active mandates for a payer.
     */
    public List<Mandate> getPayerMandates(String payerVpa) {
        return mandateRepository.findByPayerVpaOrderByCreatedAtDesc(payerVpa);
    }

    /**
     * Get mandates for a merchant.
     */
    public List<Mandate> getMerchantMandates(String merchantVpa) {
        return mandateRepository.findByMerchantVpaOrderByCreatedAtDesc(merchantVpa);
    }

    /**
     * Get pending mandates for payer approval.
     */
    public List<Mandate> getPendingApprovals(String payerVpa) {
        return mandateRepository.findByPayerVpaAndStatusOrderByCreatedAtDesc(
                payerVpa, MandateStatus.PENDING_APPROVAL);
    }

    /**
     * Expire old mandates past end date — runs daily at 1 AM.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void expireMandates() {
        List<Mandate> expired = mandateRepository.findExpiredMandates(LocalDate.now());
        for (Mandate mandate : expired) {
            mandate.setStatus(MandateStatus.EXPIRED);
            mandateRepository.save(mandate);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} mandates past end date", expired.size());
        }
    }

    // ── Internal ──

    Mandate getMandate(UUID mandateId) {
        return mandateRepository.findById(mandateId)
                .orElseThrow(() -> new IllegalArgumentException("Mandate not found"));
    }

    /**
     * Calculate next debit date based on frequency.
     */
    public LocalDate calculateNextDebitDate(LocalDate lastDebit, MandateFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> lastDebit.plusWeeks(1);
            case MONTHLY -> lastDebit.plusMonths(1);
            case QUARTERLY -> lastDebit.plusMonths(3);
            case YEARLY -> lastDebit.plusYears(1);
            case ONE_TIME -> null;
        };
    }

    private String generateMandateRef() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "MND-" + date + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
