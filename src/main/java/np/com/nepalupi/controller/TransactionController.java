package np.com.nepalupi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.dto.request.PaymentRequest;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.service.transaction.PspValidationService;
import np.com.nepalupi.service.transaction.TransactionOrchestrator;
import np.com.nepalupi.util.IdGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Transaction API — exposed to PSP apps (banks/wallets).
 * <p>
 * All endpoints require X-PSP-ID header.
 * Financial operations require X-Idempotency-Key header.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionOrchestrator orchestrator;
    private final PspValidationService pspService;

    /**
     * Initiate a P2P or P2M payment.
     * <p>
     * POST /api/v1/transactions/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<TransactionResponse> initiate(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-PSP-ID", required = false) String pspId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        // Validate PSP if provided
        if (pspId != null) {
            pspService.validate(pspId);
            request.setPspId(pspId);
        }

        // Generate idempotency key if not provided
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = IdGenerator.generateIdempotencyKey();
        }
        request.setIdempotencyKey(idempotencyKey);

        // Capture request context
        request.setIpAddress(httpRequest.getRemoteAddr());

        TransactionResponse response = orchestrator.initiatePayment(request);

        return ResponseEntity
                .status(response.isSuccess() ? 200 : 402)
                .body(response);
    }

    /**
     * Check the status of a transaction.
     * <p>
     * GET /api/v1/transactions/{upiTxnId}
     */
    @GetMapping("/{upiTxnId}")
    public ResponseEntity<TransactionResponse> getStatus(@PathVariable String upiTxnId) {
        TransactionResponse response = orchestrator.getStatus(upiTxnId);
        return ResponseEntity.ok(response);
    }
}
