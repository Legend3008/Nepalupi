package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * NCHL Callback Controller — inbound webhook receiver for async transaction
 * status updates from the NCHL/NPCI switch.
 * <p>
 * Section 3.2: Webhook receiver for callbacks from NPCI
 * <p>
 * When the NCHL switch processes a transaction asynchronously (e.g., delayed
 * bank response, reversal completion, settlement confirmation), it calls back
 * to the switch via this endpoint.
 * <p>
 * Security:
 * - HMAC-SHA256 signature verification on inbound payload
 * - Correlation by RRN (Retrieval Reference Number) or UPI Transaction ID
 * - Replay prevention via timestamp validation
 */
@RestController
@RequestMapping("/api/v1/nchl/callback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "NCHL Callbacks", description = "Inbound webhook receiver for async status updates from NCHL switch")
public class NchlCallbackController {

    private final TransactionRepository transactionRepository;

    // In production: injected from secure vault
    private static final String NCHL_WEBHOOK_SECRET = "nchl-webhook-secret-key";

    /**
     * Receive transaction status callback from NCHL.
     * <p>
     * Called when:
     * - Bank processes a delayed debit/credit
     * - Reversal is completed/failed
     * - Settlement is confirmed
     */
    @PostMapping("/transaction-status")
    @Operation(summary = "Receive transaction status update from NCHL",
            description = "NCHL calls this when a transaction status changes asynchronously")
    public ResponseEntity<Map<String, String>> receiveTransactionStatus(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-NCHL-Signature", required = false) String signature,
            @RequestHeader(value = "X-NCHL-Timestamp", required = false) String timestamp) {

        log.info("NCHL callback received: {}", payload);

        // Verify HMAC signature
        if (signature != null && !verifySignature(payload.toString(), signature, timestamp)) {
            log.warn("NCHL callback signature verification FAILED");
            return ResponseEntity.status(401)
                    .body(Map.of("status", "REJECTED", "reason", "Invalid signature"));
        }

        // Replay prevention — reject callbacks older than 5 minutes
        if (timestamp != null) {
            long callbackTime = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() - callbackTime) > 300_000) {
                log.warn("NCHL callback rejected: timestamp too old");
                return ResponseEntity.status(400)
                        .body(Map.of("status", "REJECTED", "reason", "Timestamp expired"));
            }
        }

        // Extract fields
        String rrn = (String) payload.get("rrn");
        String upiTxnId = (String) payload.get("upiTxnId");
        String status = (String) payload.get("status");
        String errorCode = (String) payload.get("errorCode");
        String errorMessage = (String) payload.get("errorMessage");

        // Correlate by RRN or UPI Transaction ID
        Transaction txn = null;
        if (upiTxnId != null) {
            txn = transactionRepository.findByUpiTxnId(upiTxnId).orElse(null);
        }
        if (txn == null && rrn != null) {
            txn = transactionRepository.findByRrn(rrn).orElse(null);
        }

        if (txn == null) {
            log.warn("NCHL callback: transaction not found for rrn={} upiTxnId={}", rrn, upiTxnId);
            return ResponseEntity.ok(Map.of("status", "NOT_FOUND",
                    "message", "Transaction not found — may need manual reconciliation"));
        }

        // Update transaction based on callback status
        log.info("NCHL callback matched txn {}: current={}, callback={}",
                txn.getUpiTxnId(), txn.getStatus(), status);

        if ("COMPLETED".equals(status) && txn.getStatus() != TransactionStatus.COMPLETED) {
            txn.setStatus(TransactionStatus.COMPLETED);
            txn.setCompletedAt(Instant.now());
            transactionRepository.save(txn);
            log.info("Transaction {} updated to COMPLETED via NCHL callback", txn.getUpiTxnId());
        } else if ("REVERSED".equals(status) && txn.getStatus() != TransactionStatus.REVERSED) {
            txn.setStatus(TransactionStatus.REVERSED);
            txn.setFailureCode(errorCode);
            txn.setFailureReason("Reversed via NCHL callback: " + errorMessage);
            transactionRepository.save(txn);
            log.info("Transaction {} updated to REVERSED via NCHL callback", txn.getUpiTxnId());
        } else if ("FAILED".equals(status)) {
            txn.setFailureCode(errorCode);
            txn.setFailureReason(errorMessage);
            transactionRepository.save(txn);
            log.warn("Transaction {} failed via NCHL callback: {} — {}",
                    txn.getUpiTxnId(), errorCode, errorMessage);
        }

        return ResponseEntity.ok(Map.of(
                "status", "ACKNOWLEDGED",
                "upiTxnId", txn.getUpiTxnId(),
                "rrn", txn.getRrn() != null ? txn.getRrn() : ""
        ));
    }

    /**
     * Receive settlement confirmation callback from NCHL.
     */
    @PostMapping("/settlement-confirmation")
    @Operation(summary = "Receive settlement confirmation from NCHL",
            description = "NCHL confirms that net settlement has been processed via NRB-IPS")
    public ResponseEntity<Map<String, String>> receiveSettlementConfirmation(
            @RequestBody Map<String, Object> payload) {

        log.info("NCHL settlement confirmation received: {}", payload);

        String settlementRef = (String) payload.get("settlementRef");
        String status = (String) payload.get("status");

        log.info("Settlement {} confirmed by NCHL with status: {}", settlementRef, status);

        return ResponseEntity.ok(Map.of(
                "status", "ACKNOWLEDGED",
                "settlementRef", settlementRef != null ? settlementRef : ""
        ));
    }

    // ── Security ──

    private boolean verifySignature(String payload, String receivedSignature, String timestamp) {
        try {
            String data = timestamp + ":" + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(NCHL_WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(hash);
            return expected.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
