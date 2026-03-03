package np.com.nepalupi.service.transaction;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.TaxPayment;
import np.com.nepalupi.repository.TaxPaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Section 19.6: Tax/Government Payment Service.
 * <p>
 * Enables UPI-based government tax payments to Nepal's Inland Revenue Department (IRD):
 * <ul>
 *   <li>Income Tax payment</li>
 *   <li>VAT (Value Added Tax) payment</li>
 *   <li>TDS (Tax Deducted at Source) payment</li>
 *   <li>Custom Duty payment</li>
 *   <li>Vehicle Tax payment</li>
 *   <li>PAN-based taxpayer lookup</li>
 *   <li>Fiscal year and period tracking (Bikram Sambat)</li>
 *   <li>Fine and interest calculation</li>
 *   <li>Payment receipt generation</li>
 * </ul>
 */
@Service
@Slf4j
public class TaxPaymentService {

    private final TaxPaymentRepository taxPaymentRepository;
    private final LimitValidationService limitValidationService;
    private final Counter taxPaymentCounter;

    public TaxPaymentService(TaxPaymentRepository taxPaymentRepository,
                              LimitValidationService limitValidationService,
                              MeterRegistry meterRegistry) {
        this.taxPaymentRepository = taxPaymentRepository;
        this.limitValidationService = limitValidationService;
        this.taxPaymentCounter = meterRegistry.counter("npi.tax.payments");
    }

    /**
     * Initiate a tax payment.
     *
     * @param userId       the user making the payment
     * @param taxType      INCOME_TAX, VAT, TDS, CUSTOM_DUTY, VEHICLE_TAX
     * @param pan          taxpayer PAN number
     * @param name         taxpayer name
     * @param fiscalYear   Nepali fiscal year (e.g., "2081/82")
     * @param taxPeriod    tax period (e.g., "Q1", "ANNUAL")
     * @param amountPaisa  tax amount in paisa
     * @param finePaisa    late filing fine
     * @param interestPaisa interest on due amount
     * @param irdOfficeCode IRD office code
     */
    @Transactional
    public TaxPayment initiateTaxPayment(UUID userId, String taxType, String pan, String name,
                                          String fiscalYear, String taxPeriod,
                                          Long amountPaisa, Long finePaisa, Long interestPaisa,
                                          String irdOfficeCode) {

        // Validate tax type
        validateTaxType(taxType);

        // Calculate total
        Long totalAmount = amountPaisa + (finePaisa != null ? finePaisa : 0) 
                           + (interestPaisa != null ? interestPaisa : 0);

        // Validate limits
        limitValidationService.validate(userId, totalAmount);

        // Generate voucher number
        String voucherNumber = generateVoucherNumber(taxType);

        TaxPayment payment = TaxPayment.builder()
                .userId(userId)
                .taxType(taxType)
                .taxpayerPan(pan)
                .taxpayerName(name)
                .fiscalYear(fiscalYear)
                .taxPeriod(taxPeriod)
                .amountPaisa(amountPaisa)
                .finePaisa(finePaisa != null ? finePaisa : 0L)
                .interestPaisa(interestPaisa != null ? interestPaisa : 0L)
                .totalAmountPaisa(totalAmount)
                .irdOfficeCode(irdOfficeCode)
                .voucherNumber(voucherNumber)
                .status("PENDING")
                .build();

        TaxPayment saved = taxPaymentRepository.save(payment);
        taxPaymentCounter.increment();

        log.info("Tax payment initiated: id={}, type={}, pan={}, amount={}, voucher={}",
                saved.getId(), taxType, pan, totalAmount, voucherNumber);

        return saved;
    }

    /**
     * Confirm tax payment after bank debit succeeds.
     */
    @Transactional
    public TaxPayment confirmPayment(UUID paymentId, UUID transactionId) {
        TaxPayment payment = taxPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Tax payment not found: " + paymentId));

        String receiptNumber = generateReceiptNumber(payment.getTaxType());
        payment.setStatus("COMPLETED");
        payment.setTransactionId(transactionId);
        payment.setPaymentReceiptNumber(receiptNumber);
        payment.setVerifiedAt(Instant.now());

        log.info("Tax payment confirmed: id={}, receipt={}", paymentId, receiptNumber);
        return taxPaymentRepository.save(payment);
    }

    /**
     * Get tax payment history for a PAN.
     */
    public List<TaxPayment> getPaymentsByPan(String pan) {
        return taxPaymentRepository.findByTaxpayerPan(pan);
    }

    /**
     * Get tax payments for a PAN in a fiscal year.
     */
    public List<TaxPayment> getPaymentsByPanAndYear(String pan, String fiscalYear) {
        return taxPaymentRepository.findByPanAndFiscalYear(pan, fiscalYear);
    }

    /**
     * Get paginated tax payments for a user.
     */
    public Page<TaxPayment> getUserPayments(UUID userId, Pageable pageable) {
        return taxPaymentRepository.findByUserId(userId, pageable);
    }

    /**
     * Look up payment by voucher number.
     */
    public TaxPayment getByVoucher(String voucherNumber) {
        return taxPaymentRepository.findByVoucherNumber(voucherNumber)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found: " + voucherNumber));
    }

    /**
     * Calculate fine for late filing.
     */
    public Long calculateLateFine(String taxType, Long amountPaisa, int daysLate) {
        // NRB/IRD fine rates
        double fineRate = switch (taxType) {
            case "INCOME_TAX" -> 0.0005;  // 0.05% per day
            case "VAT"        -> 0.001;   // 0.1% per day
            case "TDS"        -> 0.0015;  // 0.15% per day
            default           -> 0.001;
        };
        return (long) (amountPaisa * fineRate * daysLate);
    }

    private void validateTaxType(String taxType) {
        List<String> validTypes = List.of("INCOME_TAX", "VAT", "TDS", "CUSTOM_DUTY", "VEHICLE_TAX");
        if (!validTypes.contains(taxType)) {
            throw new IllegalArgumentException("Invalid tax type: " + taxType + 
                    ". Valid types: " + validTypes);
        }
    }

    private String generateVoucherNumber(String taxType) {
        String prefix = switch (taxType) {
            case "INCOME_TAX"  -> "IT";
            case "VAT"         -> "VAT";
            case "TDS"         -> "TDS";
            case "CUSTOM_DUTY" -> "CD";
            case "VEHICLE_TAX" -> "VT";
            default            -> "TX";
        };
        return prefix + "-" + System.currentTimeMillis() + "-" + 
                UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String generateReceiptNumber(String taxType) {
        return "RCP-" + taxType.substring(0, 2) + "-" + System.currentTimeMillis();
    }
}
