package np.com.nepalupi.mandate.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.mandate.entity.CollectRequest;
import np.com.nepalupi.mandate.service.CollectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Collect (Pull Payment) Controller — Module 12.
 * <p>
 * POST /api/v1/collect/request            → Create collect request
 * POST /api/v1/collect/{id}/approve        → Approve (payer pays)
 * POST /api/v1/collect/{id}/reject         → Reject
 * GET  /api/v1/collect/pending/{payerVpa}  → Payer's pending requests
 * GET  /api/v1/collect/sent/{requestorVpa} → Requestor's sent requests
 */
@RestController
@RequestMapping("/api/v1/collect")
@RequiredArgsConstructor
@Tag(name = "Collect", description = "UPI Collect (pull payment) requests")
public class CollectController {

    private final CollectService collectService;

    @PostMapping("/request")
    public ResponseEntity<CollectRequest> createRequest(
            @RequestParam String requestorVpa,
            @RequestParam String payerVpa,
            @RequestParam Long amountPaisa,
            @RequestParam(required = false) String description) {
        return ResponseEntity.ok(collectService.createCollectRequest(
                requestorVpa, payerVpa, amountPaisa, description));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<CollectRequest> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(collectService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<CollectRequest> reject(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(collectService.reject(id, reason));
    }

    @GetMapping("/pending/{payerVpa}")
    public ResponseEntity<List<CollectRequest>> getPending(@PathVariable String payerVpa) {
        return ResponseEntity.ok(collectService.getPendingRequests(payerVpa));
    }

    @GetMapping("/sent/{requestorVpa}")
    public ResponseEntity<List<CollectRequest>> getSent(@PathVariable String requestorVpa) {
        return ResponseEntity.ok(collectService.getSentRequests(requestorVpa));
    }
}
