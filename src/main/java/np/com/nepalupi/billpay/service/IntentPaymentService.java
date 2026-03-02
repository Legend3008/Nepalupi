package np.com.nepalupi.billpay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.billpay.entity.IntentPayment;
import np.com.nepalupi.billpay.repository.IntentPaymentRepository;
import np.com.nepalupi.domain.dto.request.PaymentRequest;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.service.transaction.TransactionOrchestrator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Intent Payment Service — Handles UPI Intent-based payments.
 * <p>
 * UPI Intent flow (Section 2.4 of spec):
 * 1. Merchant generates a payment intent with amount, VPA, and note
 * 2. System creates intent_payment record and generates upi://pay? deep-link URL
 * 3. User's PSP app opens when they click the deep-link
 * 4. User reviews payment details and authorizes with MPIN
 * 5. Payment is routed through standard TransactionOrchestrator
 * 6. Result is returned to merchant via callback
 * <p>
 * Intent URL format (Nepal UPI):
 *   upi://pay?pa={merchantVpa}&pn={merchantName}&am={amount}&cu=NPR&tn={note}&tr={intentRef}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntentPaymentService {

    private static final int INTENT_EXPIRY_MINUTES = 15;

    private final IntentPaymentRepository intentPaymentRepository;
    private final TransactionOrchestrator transactionOrchestrator;

    /**
     * Create a new payment intent (merchant-initiated).
     *
     * @param merchantVpa  Merchant's VPA (e.g. shop@nchl)
     * @param merchantName Display name of the merchant
     * @param amountPaisa  Amount in paisa (NPR × 100)
     * @param note         Payment note/description
     * @return IntentPayment with generated intent URL
     */
    @Transactional
    public IntentPayment createIntent(String merchantVpa, String merchantName,
                                       Long amountPaisa, String note) {
        String intentRef = "INT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String intentUrl = buildIntentUrl(merchantVpa, merchantName, amountPaisa, note, intentRef);

        IntentPayment intent = IntentPayment.builder()
                .intentRef(intentRef)
                .merchantVpa(merchantVpa)
                .merchantName(merchantName)
                .amountPaisa(amountPaisa)
                .note(note)
                .intentUrl(intentUrl)
                .status("CREATED")
                .expiresAt(Instant.now().plus(INTENT_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .build();

        intent = intentPaymentRepository.save(intent);
        log.info("Intent payment created: ref={}, merchant={}, amount={} paisa",
                intentRef, merchantVpa, amountPaisa);
        return intent;
    }

    /**
     * Authorize an intent — called when user's PSP app opens the deep-link
     * and user reviews the payment.
     *
     * @param intentRef The intent reference from the deep-link
     * @param payerVpa  The payer's VPA who clicked the link
     * @return IntentPayment in AUTHORIZED status
     */
    @Transactional
    public IntentPayment authorizeIntent(String intentRef, String payerVpa) {
        IntentPayment intent = intentPaymentRepository.findByIntentRef(intentRef)
                .orElseThrow(() -> new IllegalArgumentException("Intent not found: " + intentRef));

        if (!"CREATED".equals(intent.getStatus())) {
            throw new IllegalStateException("Intent is not in CREATED status: " + intent.getStatus());
        }

        if (intent.getExpiresAt().isBefore(Instant.now())) {
            intent.setStatus("EXPIRED");
            intentPaymentRepository.save(intent);
            throw new IllegalStateException("Intent has expired: " + intentRef);
        }

        intent.setPayerVpa(payerVpa);
        intent.setStatus("AUTHORIZED");
        intent = intentPaymentRepository.save(intent);

        log.info("Intent authorized: ref={}, payer={}", intentRef, payerVpa);
        return intent;
    }

    /**
     * Complete an intent payment — called after user enters MPIN.
     * Routes through the standard UPI TransactionOrchestrator.
     *
     * @param intentRef         Intent reference
     * @param pspId             PSP ID of the payer
     * @param deviceFingerprint Device fingerprint
     * @param ipAddress         IP address
     * @return IntentPayment with final status
     */
    @Transactional
    public IntentPayment completeIntentPayment(String intentRef, String pspId,
                                                String deviceFingerprint, String ipAddress) {
        IntentPayment intent = intentPaymentRepository.findByIntentRef(intentRef)
                .orElseThrow(() -> new IllegalArgumentException("Intent not found: " + intentRef));

        if (!"AUTHORIZED".equals(intent.getStatus())) {
            throw new IllegalStateException("Intent must be AUTHORIZED before payment: " + intent.getStatus());
        }

        if (intent.getExpiresAt().isBefore(Instant.now())) {
            intent.setStatus("EXPIRED");
            intentPaymentRepository.save(intent);
            throw new IllegalStateException("Intent has expired: " + intentRef);
        }

        // Route through standard UPI payment flow
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .payerVpa(intent.getPayerVpa())
                .payeeVpa(intent.getMerchantVpa())
                .amount(intent.getAmountPaisa())
                .note(intent.getNote())
                .pspId(pspId)
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .idempotencyKey("INTENT-" + intent.getIntentRef())
                .build();

        TransactionResponse response = transactionOrchestrator.initiatePayment(paymentRequest);

        if (response.getStatus() == TransactionStatus.COMPLETED) {
            intent.setStatus("COMPLETED");
            intent.setTransactionId(null); // Transaction tracked via upiTxnId
            log.info("Intent payment completed: ref={}, txn={}", intentRef, response.getUpiTxnId());
        } else {
            intent.setStatus("FAILED");
            log.warn("Intent payment failed: ref={}, reason={}", intentRef, response.getFailureReason());
        }

        return intentPaymentRepository.save(intent);
    }

    /**
     * Get intent details by reference.
     */
    public IntentPayment getByIntentRef(String intentRef) {
        return intentPaymentRepository.findByIntentRef(intentRef)
                .orElseThrow(() -> new IllegalArgumentException("Intent not found: " + intentRef));
    }

    /**
     * Expire stale intents — runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void expireStaleIntents() {
        List<IntentPayment> expired = intentPaymentRepository.findExpiredIntents(Instant.now());
        for (IntentPayment intent : expired) {
            intent.setStatus("EXPIRED");
            intentPaymentRepository.save(intent);
            log.info("Intent expired: ref={}", intent.getIntentRef());
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} stale intent payments", expired.size());
        }
    }

    // ── Internal ──

    /**
     * Build Nepal UPI intent deep-link URL.
     * Format: upi://pay?pa={vpa}&pn={name}&am={amount}&cu=NPR&tn={note}&tr={ref}
     */
    private String buildIntentUrl(String merchantVpa, String merchantName,
                                   Long amountPaisa, String note, String intentRef) {
        // Convert paisa to NPR for the URL (UPI intent uses decimal amount)
        String amountNpr = String.format("%.2f", amountPaisa / 100.0);

        return "upi://pay?"
                + "pa=" + encode(merchantVpa)
                + "&pn=" + encode(merchantName)
                + "&am=" + amountNpr
                + "&cu=NPR"
                + "&tn=" + encode(note != null ? note : "Payment")
                + "&tr=" + encode(intentRef);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
