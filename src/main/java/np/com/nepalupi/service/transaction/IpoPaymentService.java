package np.com.nepalupi.service.transaction;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.IpoPayment;
import np.com.nepalupi.repository.IpoPaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Section 19.5: IPO/FPO Payment Service.
 * <p>
 * Enables UPI-based share application for Initial Public Offerings (IPOs)
 * and Further Public Offerings (FPOs) issued through SEBON (Securities Board of Nepal):
 * <ul>
 *   <li>Apply for IPO with DMAT number, BOID, bank details</li>
 *   <li>Block amount in user's bank account</li>
 *   <li>Verify application with CDSC (Central Depository System)</li>
 *   <li>Handle allotment and automatic refund for unallotted kitta</li>
 *   <li>Duplicate application detection per BOID per IPO</li>
 * </ul>
 */
@Service
@Slf4j
public class IpoPaymentService {

    private final IpoPaymentRepository ipoPaymentRepository;
    private final LimitValidationService limitValidationService;
    private final Counter ipoApplicationCounter;
    private final Counter ipoSuccessCounter;

    public IpoPaymentService(IpoPaymentRepository ipoPaymentRepository,
                              LimitValidationService limitValidationService,
                              MeterRegistry meterRegistry) {
        this.ipoPaymentRepository = ipoPaymentRepository;
        this.limitValidationService = limitValidationService;
        this.ipoApplicationCounter = meterRegistry.counter("npi.ipo.applications", "type", "total");
        this.ipoSuccessCounter = meterRegistry.counter("npi.ipo.applications", "type", "success");
    }

    /**
     * Apply for an IPO.
     */
    @Transactional
    public IpoPayment applyForIpo(UUID userId, String ipoCode, String ipoName, String companyName,
                                    int kittaApplied, Long amountPerKittaPaisa,
                                    String bankCode, String accountNumber,
                                    String dematNumber, String boid) {

        ipoApplicationCounter.increment();

        // 1. Check for duplicate application
        ipoPaymentRepository.findByBoidAndIpoCode(boid, ipoCode).ifPresent(existing -> {
            throw new IllegalStateException(
                    "Duplicate IPO application: BOID " + boid + " already applied for " + ipoCode);
        });

        // 2. Calculate total amount
        Long totalAmount = (long) kittaApplied * amountPerKittaPaisa;

        // 3. Validate against transaction limits
        limitValidationService.validate(userId, totalAmount);

        // 4. Create application
        IpoPayment payment = IpoPayment.builder()
                .userId(userId)
                .ipoCode(ipoCode)
                .ipoName(ipoName)
                .companyName(companyName)
                .kittaApplied(kittaApplied)
                .amountPerKittaPaisa(amountPerKittaPaisa)
                .totalAmountPaisa(totalAmount)
                .bankCode(bankCode)
                .accountNumber(accountNumber)
                .dematNumber(dematNumber)
                .boid(boid)
                .status("PENDING")
                .appliedAt(Instant.now())
                .build();

        IpoPayment saved = ipoPaymentRepository.save(payment);
        log.info("IPO application created: id={}, ipo={}, boid={}, kitta={}, amount={}",
                saved.getId(), ipoCode, boid, kittaApplied, totalAmount);

        // 5. In production: initiate amount blocking via bank CBS
        // bankIntegration.blockAmount(bankCode, accountNumber, totalAmount);

        ipoSuccessCounter.increment();
        return saved;
    }

    /**
     * Verify an IPO application (after CDSC confirms).
     */
    @Transactional
    public IpoPayment verifyApplication(UUID paymentId, String applicationNumber) {
        IpoPayment payment = ipoPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("IPO payment not found: " + paymentId));

        payment.setStatus("VERIFIED");
        payment.setApplicationNumber(applicationNumber);
        payment.setVerifiedAt(Instant.now());

        log.info("IPO application verified: id={}, appNo={}", paymentId, applicationNumber);
        return ipoPaymentRepository.save(payment);
    }

    /**
     * Process allotment result.
     */
    @Transactional
    public IpoPayment processAllotment(UUID paymentId, int allottedKitta) {
        IpoPayment payment = ipoPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("IPO payment not found: " + paymentId));

        payment.setAllotmentKitta(allottedKitta);

        if (allottedKitta == 0) {
            // Full refund
            payment.setStatus("REFUNDED");
            payment.setRefundAmountPaisa(payment.getTotalAmountPaisa());
            payment.setRefundAt(Instant.now());
            log.info("IPO fully refunded: id={}, amount={}", paymentId, payment.getTotalAmountPaisa());
        } else if (allottedKitta < payment.getKittaApplied()) {
            // Partial allotment — refund unallotted
            long allottedAmount = (long) allottedKitta * payment.getAmountPerKittaPaisa();
            long refundAmount = payment.getTotalAmountPaisa() - allottedAmount;
            payment.setStatus("PARTIALLY_ALLOTTED");
            payment.setRefundAmountPaisa(refundAmount);
            payment.setRefundAt(Instant.now());
            log.info("IPO partially allotted: id={}, allotted={}/{}, refund={}",
                    paymentId, allottedKitta, payment.getKittaApplied(), refundAmount);
        } else {
            // Full allotment
            payment.setStatus("ALLOTTED");
            log.info("IPO fully allotted: id={}, kitta={}", paymentId, allottedKitta);
        }

        return ipoPaymentRepository.save(payment);
    }

    /**
     * Get user's IPO applications.
     */
    public List<IpoPayment> getUserApplications(UUID userId) {
        return ipoPaymentRepository.findByUserId(userId);
    }

    /**
     * Get paginated IPO applications.
     */
    public Page<IpoPayment> getUserApplicationsPaged(UUID userId, Pageable pageable) {
        return ipoPaymentRepository.findByUserId(userId, pageable);
    }

    /**
     * Get total applications for an IPO.
     */
    public long getApplicationCount(String ipoCode) {
        return ipoPaymentRepository.countActiveApplications(ipoCode);
    }
}
