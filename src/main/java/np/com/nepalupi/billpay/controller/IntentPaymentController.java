package np.com.nepalupi.billpay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.billpay.entity.IntentPayment;
import np.com.nepalupi.billpay.service.IntentPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Intent Payment API — UPI Intent-based payments for online merchant integration.
 * <p>
 * Flow:
 * 1. Merchant calls POST /create to generate a payment intent with deep-link URL
 * 2. User clicks the deep-link → PSP app opens → calls POST /authorize
 * 3. User enters MPIN → PSP calls POST /complete
 * 4. Merchant polls GET /status to check result
 */
@RestController
@RequestMapping("/api/v1/intent")
@RequiredArgsConstructor
@Tag(name = "Intent Payments", description = "UPI Intent deep-link payment flow for online merchants")
public class IntentPaymentController {

    private final IntentPaymentService intentPaymentService;

    @PostMapping("/create")
    @Operation(summary = "Create payment intent", description = "Merchant creates intent — returns deep-link URL (upi://pay?...)")
    public ResponseEntity<IntentPayment> createIntent(@RequestBody Map<String, Object> request) {
        IntentPayment intent = intentPaymentService.createIntent(
                (String) request.get("merchantVpa"),
                (String) request.get("merchantName"),
                ((Number) request.get("amountPaisa")).longValue(),
                (String) request.get("note")
        );
        return ResponseEntity.ok(intent);
    }

    @PostMapping("/authorize/{intentRef}")
    @Operation(summary = "Authorize intent", description = "Called when user's PSP app opens the deep-link — user reviews payment")
    public ResponseEntity<IntentPayment> authorizeIntent(
            @PathVariable String intentRef,
            @RequestBody Map<String, String> request) {
        IntentPayment intent = intentPaymentService.authorizeIntent(
                intentRef,
                request.get("payerVpa")
        );
        return ResponseEntity.ok(intent);
    }

    @PostMapping("/complete/{intentRef}")
    @Operation(summary = "Complete intent payment", description = "Called after user enters MPIN — routes through UPI TransactionOrchestrator")
    public ResponseEntity<IntentPayment> completePayment(
            @PathVariable String intentRef,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-PSP-ID", required = false) String pspId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        IntentPayment completed = intentPaymentService.completeIntentPayment(
                intentRef,
                pspId != null ? pspId : request.get("pspId"),
                request.get("deviceFingerprint"),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(completed);
    }

    @GetMapping("/status/{intentRef}")
    @Operation(summary = "Check intent status", description = "Merchant polls this to check payment result")
    public ResponseEntity<IntentPayment> getStatus(@PathVariable String intentRef) {
        return ResponseEntity.ok(intentPaymentService.getByIntentRef(intentRef));
    }
}
