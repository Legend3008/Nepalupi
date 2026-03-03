package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.service.transaction.AlternatePaymentService;
import np.com.nepalupi.service.vpa.MobileVpaLookupService;
import np.com.nepalupi.util.IdGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Extended payment endpoints — send via mobile, send via account+bank code.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Extended Payments", description = "Send money via mobile number or bank account")
public class PaymentController {

    private final AlternatePaymentService alternatePaymentService;
    private final MobileVpaLookupService mobileVpaLookupService;

    @PostMapping("/send-via-mobile")
    @Operation(summary = "Send via mobile", description = "Send money using recipient's mobile number")
    public ResponseEntity<TransactionResponse> sendViaMobile(
            @RequestParam String payerVpa,
            @RequestParam String payeeMobile,
            @RequestParam Long amount,
            @RequestParam(required = false) String note,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request) {

        if (idempotencyKey == null) idempotencyKey = IdGenerator.generateIdempotencyKey();

        TransactionResponse response = alternatePaymentService.sendViaMobile(
                payerVpa, payeeMobile, amount, note, idempotencyKey, request.getRemoteAddr());

        return ResponseEntity.status(response.isSuccess() ? 200 : 402).body(response);
    }

    @PostMapping("/send-via-account")
    @Operation(summary = "Send via account", description = "Send money using recipient's bank account + bank code")
    public ResponseEntity<TransactionResponse> sendViaAccount(
            @RequestParam String payerVpa,
            @RequestParam String payeeBankCode,
            @RequestParam String payeeAccountNumber,
            @RequestParam Long amount,
            @RequestParam(required = false) String note,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request) {

        if (idempotencyKey == null) idempotencyKey = IdGenerator.generateIdempotencyKey();

        TransactionResponse response = alternatePaymentService.sendViaAccount(
                payerVpa, payeeBankCode, payeeAccountNumber, amount, note, idempotencyKey, request.getRemoteAddr());

        return ResponseEntity.status(response.isSuccess() ? 200 : 402).body(response);
    }

    @GetMapping("/resolve-mobile/{mobile}")
    @Operation(summary = "Resolve mobile to VPA", description = "Look up the primary VPA linked to a mobile number")
    public ResponseEntity<Map<String, Object>> resolveMobile(@PathVariable String mobile) {
        return ResponseEntity.ok(mobileVpaLookupService.resolveByMobile(mobile));
    }
}
