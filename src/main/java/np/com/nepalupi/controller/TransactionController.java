package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.dto.request.PaymentRequest;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.service.transaction.BalanceEnquiryService;
import np.com.nepalupi.service.transaction.PspValidationService;
import np.com.nepalupi.service.transaction.TransactionOrchestrator;
import np.com.nepalupi.util.IdGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Transaction API — exposed to PSP apps (banks/wallets).
 * <p>
 * All endpoints require X-PSP-ID header (enforced by PspAuthFilter).
 * Financial operations require X-Idempotency-Key header.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "UPI payment operations — P2P, P2M, balance, refunds")
public class TransactionController {

    private final TransactionOrchestrator orchestrator;
    private final PspValidationService pspService;
    private final TransactionRepository transactionRepository;
    private final BalanceEnquiryService balanceEnquiryService;

    /**
     * Initiate a P2P or P2M payment.
     */
    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Start a P2P or P2M UPI push payment")
    public ResponseEntity<TransactionResponse> initiate(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-PSP-ID", required = false) String pspId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        if (pspId != null) {
            pspService.validate(pspId);
            request.setPspId(pspId);
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = IdGenerator.generateIdempotencyKey();
        }
        request.setIdempotencyKey(idempotencyKey);
        request.setIpAddress(httpRequest.getRemoteAddr());

        TransactionResponse response = orchestrator.initiatePayment(request);

        return ResponseEntity
                .status(response.isSuccess() ? 200 : 402)
                .body(response);
    }

    /**
     * Check the status of a transaction.
     */
    @GetMapping("/{upiTxnId}")
    @Operation(summary = "Get transaction status", description = "Check the current status of a transaction by UPI Txn ID")
    public ResponseEntity<TransactionResponse> getStatus(
            @Parameter(description = "UPI Transaction ID (e.g., NPL2025...)") @PathVariable String upiTxnId) {
        TransactionResponse response = orchestrator.getStatus(upiTxnId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction history for a VPA (as payer or payee).
     */
    @GetMapping("/history/{vpa}")
    @Operation(summary = "Transaction history", description = "Get all transactions for a VPA address")
    public ResponseEntity<Map<String, Object>> getHistory(
            @Parameter(description = "VPA address (e.g., ritesh@nchl)") @PathVariable String vpa,
            @RequestParam(defaultValue = "20") int limit) {

        List<TransactionResponse> asPayer = transactionRepository.findByPayerVpaOrderByInitiatedAtDesc(vpa)
                .stream()
                .limit(limit)
                .map(TransactionResponse::from)
                .toList();

        List<TransactionResponse> asPayee = transactionRepository.findByPayeeVpaOrderByInitiatedAtDesc(vpa)
                .stream()
                .limit(limit)
                .map(TransactionResponse::from)
                .toList();

        // Summary statistics
        long totalSent = transactionRepository.findByPayerVpaOrderByInitiatedAtDesc(vpa).stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .mapToLong(Transaction::getAmount)
                .sum();

        long totalReceived = transactionRepository.findByPayeeVpaOrderByInitiatedAtDesc(vpa).stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .mapToLong(Transaction::getAmount)
                .sum();

        return ResponseEntity.ok(Map.of(
                "vpa", vpa,
                "sentTransactions", asPayer,
                "receivedTransactions", asPayee,
                "totalSentPaisa", totalSent,
                "totalReceivedPaisa", totalReceived,
                "totalSentNPR", totalSent / 100.0,
                "totalReceivedNPR", totalReceived / 100.0
        ));
    }

    /**
     * Balance enquiry via VPA.
     */
    @GetMapping("/balance/{vpa}")
    @Operation(summary = "Balance enquiry", description = "Check bank account balance via VPA")
    public ResponseEntity<Map<String, Object>> checkBalance(
            @Parameter(description = "VPA address (e.g., ritesh@nchl)") @PathVariable String vpa) {
        return ResponseEntity.ok(balanceEnquiryService.checkBalance(vpa));
    }

    /**
     * Initiate a refund for a completed transaction.
     */
    @PostMapping("/{upiTxnId}/refund")
    @Operation(summary = "Initiate refund", description = "Refund a completed transaction (creates a reverse payment)")
    public ResponseEntity<TransactionResponse> refund(
            @Parameter(description = "Original UPI Txn ID to refund") @PathVariable String upiTxnId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-PSP-ID", required = false) String pspId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        Transaction original = transactionRepository.findByUpiTxnId(upiTxnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + upiTxnId));

        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("Can only refund COMPLETED transactions. Current: " + original.getStatus());
        }

        // Create reverse payment (payee → payer)
        PaymentRequest refundRequest = PaymentRequest.builder()
                .payerVpa(original.getPayeeVpa())  // Reverse: payee becomes payer
                .payeeVpa(original.getPayerVpa())   // Reverse: payer becomes payee
                .amount(original.getAmount())
                .note("REFUND: " + upiTxnId + (reason != null ? " | " + reason : ""))
                .idempotencyKey("REFUND-" + upiTxnId)
                .pspId(pspId)
                .ipAddress(httpRequest.getRemoteAddr())
                .build();

        TransactionResponse response = orchestrator.initiatePayment(refundRequest);
        return ResponseEntity.status(response.isSuccess() ? 200 : 402).body(response);
    }
}
