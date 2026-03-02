package np.com.nepalupi.billpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.billpay.entity.Bill;
import np.com.nepalupi.billpay.entity.Biller;
import np.com.nepalupi.billpay.repository.BillRepository;
import np.com.nepalupi.billpay.repository.BillerRepository;
import np.com.nepalupi.domain.dto.request.PaymentRequest;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.service.transaction.TransactionOrchestrator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bill Payment Service — Nepal's equivalent of India's BBPS (Bharat Bill Payment System).
 * <p>
 * Supports:
 * 1. Biller registration and discovery
 * 2. Bill fetch (for billers that support it)
 * 3. Bill payment via UPI
 * 4. Bill payment confirmation and receipt
 * 5. Overdue bill tracking
 * <p>
 * Flow:
 * 1. User selects biller category and biller
 * 2. User provides customer identifier (account number, consumer ID, etc.)
 * 3. System fetches outstanding bill (if biller supports fetch)
 * 4. User confirms and pays via UPI (routed through TransactionOrchestrator)
 * 5. Payment confirmation sent to biller and user
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillPaymentService {

    private final BillerRepository billerRepository;
    private final BillRepository billRepository;
    private final TransactionOrchestrator transactionOrchestrator;

    // ── Biller Management ──

    /**
     * Register a new biller in the system.
     */
    @Transactional
    public Biller registerBiller(String billerId, String billerName, String category,
                                  String subCategory, String bankCode, String settlementAccount,
                                  boolean fetchSupported) {
        Biller biller = Biller.builder()
                .billerId(billerId)
                .billerName(billerName)
                .category(category)
                .subCategory(subCategory)
                .bankCode(bankCode)
                .settlementAccount(settlementAccount)
                .fetchSupported(fetchSupported)
                .isAdhoc(false)
                .isActive(true)
                .paymentModes("ONLINE,QR")
                .build();

        biller = billerRepository.save(biller);
        log.info("Biller registered: {} ({}) category={}", billerId, billerName, category);
        return biller;
    }

    /**
     * List billers by category.
     */
    public List<Biller> listBillersByCategory(String category) {
        return billerRepository.findByCategoryAndIsActiveTrue(category);
    }

    /**
     * Search billers by name.
     */
    public List<Biller> searchBillers(String name) {
        return billerRepository.findByBillerNameContainingIgnoreCaseAndIsActiveTrue(name);
    }

    /**
     * Get all active billers.
     */
    public List<Biller> listAllBillers() {
        return billerRepository.findByIsActiveTrue();
    }

    // ── Bill Fetch & Pay ──

    /**
     * Fetch outstanding bill from a biller.
     * For billers with fetch_supported=true, this queries the biller's system.
     * For ad-hoc billers, user enters the amount manually.
     */
    @Transactional
    public Bill fetchBill(String billerId, String customerIdentifier) {
        Biller biller = billerRepository.findByBillerId(billerId)
                .orElseThrow(() -> new IllegalArgumentException("Biller not found: " + billerId));

        if (!biller.getIsActive()) {
            throw new IllegalStateException("Biller is inactive: " + billerId);
        }

        // Check for existing pending bill
        List<Bill> pending = billRepository.findByBillerIdAndCustomerIdentifierAndStatus(
                biller.getId(), customerIdentifier, "PENDING");
        if (!pending.isEmpty()) {
            log.info("Returning existing pending bill for {} at {}", customerIdentifier, billerId);
            return pending.get(0);
        }

        if (biller.getFetchSupported()) {
            // In production: call biller's API to fetch outstanding amount
            // For now: create a bill record that will be updated when biller responds
            Bill bill = Bill.builder()
                    .biller(biller)
                    .customerIdentifier(customerIdentifier)
                    .billNumber("BIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .amountPaisa(0L) // Will be updated by biller callback
                    .status("FETCHING")
                    .build();

            bill = billRepository.save(bill);
            log.info("Bill fetch initiated: biller={}, customer={}", billerId, customerIdentifier);

            // Simulate biller response (in production: async callback)
            bill.setAmountPaisa(calculateBillerAmount(biller));
            bill.setStatus("PENDING");
            bill = billRepository.save(bill);
            return bill;
        } else {
            // Ad-hoc biller — user provides amount
            log.info("Ad-hoc biller {} — user must provide amount", billerId);
            return null;
        }
    }

    /**
     * Create an ad-hoc bill (for billers that don't support fetch).
     */
    @Transactional
    public Bill createAdHocBill(String billerId, String customerIdentifier, Long amountPaisa) {
        Biller biller = billerRepository.findByBillerId(billerId)
                .orElseThrow(() -> new IllegalArgumentException("Biller not found: " + billerId));

        Bill bill = Bill.builder()
                .biller(biller)
                .customerIdentifier(customerIdentifier)
                .billNumber("BIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amountPaisa(amountPaisa)
                .status("PENDING")
                .build();

        bill = billRepository.save(bill);
        log.info("Ad-hoc bill created: biller={}, amount={} paisa", billerId, amountPaisa);
        return bill;
    }

    /**
     * Pay a bill via UPI. Routes through the standard TransactionOrchestrator.
     */
    @Transactional
    public Bill payBill(UUID billId, String payerVpa, String pspId,
                         String deviceFingerprint, String ipAddress) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        if (!"PENDING".equals(bill.getStatus())) {
            throw new IllegalStateException("Bill is not in PENDING status: " + bill.getStatus());
        }

        Biller biller = bill.getBiller();

        // Route payment through UPI transaction engine
        // The biller's settlement account VPA is the payee
        String billerVpa = biller.getBillerId() + "@" + biller.getBankCode().toLowerCase();

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .payerVpa(payerVpa)
                .payeeVpa(billerVpa)
                .amount(bill.getAmountPaisa())
                .note("Bill payment: " + biller.getBillerName() + " - " + bill.getBillNumber())
                .pspId(pspId)
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .idempotencyKey("BILL-" + billId)
                .build();

        TransactionResponse response = transactionOrchestrator.initiatePayment(paymentRequest);

        if (response.getStatus() == TransactionStatus.COMPLETED) {
            bill.setStatus("PAID");
            bill.setPaidAt(Instant.now());
            bill.setPayerVpa(payerVpa);
            bill.setTransactionId(null); // Transaction ID tracked via upiTxnId
            log.info("Bill payment successful: bill={}, txn={}", bill.getBillNumber(), response.getUpiTxnId());
        } else {
            bill.setStatus("FAILED");
            log.warn("Bill payment failed: bill={}, reason={}", bill.getBillNumber(), response.getFailureReason());
        }

        return billRepository.save(bill);
    }

    /**
     * Get bill payment history for a user.
     */
    public List<Bill> getPaymentHistory(String payerVpa) {
        return billRepository.findByPayerVpa(payerVpa);
    }

    /**
     * Mark overdue bills — runs daily at 6 AM.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Kathmandu")
    @Transactional
    public void markOverdueBills() {
        List<Bill> overdue = billRepository.findOverdueBills();
        if (!overdue.isEmpty()) {
            log.info("Found {} overdue bills", overdue.size());
        }
    }

    // ── Internal ──

    /**
     * Simulate biller amount calculation.
     * In production, this would be the biller's API response.
     */
    private Long calculateBillerAmount(Biller biller) {
        // In production: actual biller API call
        return switch (biller.getCategory()) {
            case "ELECTRICITY" -> 250000L;  // Rs 2,500
            case "WATER" -> 80000L;         // Rs 800
            case "TELECOM" -> 50000L;       // Rs 500
            case "GAS" -> 120000L;          // Rs 1,200
            case "BROADBAND" -> 150000L;    // Rs 1,500
            default -> 100000L;             // Rs 1,000 default
        };
    }
}
