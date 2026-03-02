package np.com.nepalupi.mandate.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.mandate.entity.Mandate;
import np.com.nepalupi.mandate.entity.MandateExecution;
import np.com.nepalupi.mandate.enums.MandateFrequency;
import np.com.nepalupi.mandate.service.MandateExecutionService;
import np.com.nepalupi.mandate.service.MandateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Mandate (Recurring Payments / Autopay) Controller — Module 12.
 * <p>
 * POST /api/v1/mandates/create              → Create mandate
 * POST /api/v1/mandates/{id}/approve         → Approve (payer)
 * POST /api/v1/mandates/{id}/pause           → Pause
 * POST /api/v1/mandates/{id}/resume          → Resume
 * POST /api/v1/mandates/{id}/cancel          → Cancel
 * POST /api/v1/mandates/{id}/revoke-cooling  → Revoke one-time during cooling
 * GET  /api/v1/mandates/payer/{vpa}          → Payer's mandates
 * GET  /api/v1/mandates/merchant/{vpa}       → Merchant's mandates
 * GET  /api/v1/mandates/pending/{payerVpa}   → Pending approvals
 * GET  /api/v1/mandates/{id}/executions      → Execution history
 */
@RestController
@RequestMapping("/api/v1/mandates")
@RequiredArgsConstructor
@Tag(name = "Mandates", description = "UPI Autopay — recurring payment mandates")
public class MandateController {

    private final MandateService mandateService;
    private final MandateExecutionService executionService;

    @PostMapping("/create")
    public ResponseEntity<Mandate> createMandate(
            @RequestParam String merchantVpa,
            @RequestParam String payerVpa,
            @RequestParam(required = false) Long amountPaisa,
            @RequestParam Long maxAmountPaisa,
            @RequestParam MandateFrequency frequency,
            @RequestParam String category,
            @RequestParam(required = false) String purpose,
            @RequestParam LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean oneTime) {
        return ResponseEntity.ok(mandateService.createMandate(
                merchantVpa, payerVpa, amountPaisa, maxAmountPaisa,
                frequency, category, purpose, startDate, endDate, oneTime));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Mandate> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(mandateService.approve(id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Mandate> pause(@PathVariable UUID id) {
        return ResponseEntity.ok(mandateService.pause(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Mandate> resume(@PathVariable UUID id) {
        return ResponseEntity.ok(mandateService.resume(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Mandate> cancel(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(mandateService.cancel(id, reason));
    }

    @PostMapping("/{id}/revoke-cooling")
    public ResponseEntity<Mandate> revokeDuringCooling(@PathVariable UUID id) {
        return ResponseEntity.ok(mandateService.revokeDuringCooling(id));
    }

    @GetMapping("/payer/{vpa}")
    public ResponseEntity<List<Mandate>> getPayerMandates(@PathVariable String vpa) {
        return ResponseEntity.ok(mandateService.getPayerMandates(vpa));
    }

    @GetMapping("/merchant/{vpa}")
    public ResponseEntity<List<Mandate>> getMerchantMandates(@PathVariable String vpa) {
        return ResponseEntity.ok(mandateService.getMerchantMandates(vpa));
    }

    @GetMapping("/pending/{payerVpa}")
    public ResponseEntity<List<Mandate>> getPendingApprovals(@PathVariable String payerVpa) {
        return ResponseEntity.ok(mandateService.getPendingApprovals(payerVpa));
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<MandateExecution>> getExecutions(@PathVariable UUID id) {
        return ResponseEntity.ok(executionService.getExecutionHistory(id));
    }
}
