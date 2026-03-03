package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.Chargeback;
import np.com.nepalupi.service.dispute.ChargebackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Chargeback management API.
 */
@RestController
@RequestMapping("/api/v1/chargebacks")
@RequiredArgsConstructor
public class ChargebackController {

    private final ChargebackService chargebackService;

    @PostMapping
    public ResponseEntity<Chargeback> initiate(@RequestBody Map<String, Object> request) {
        UUID disputeId = UUID.fromString((String) request.get("disputeId"));
        Long amount = Long.valueOf(request.get("chargebackAmount").toString());
        String reason = (String) request.get("reason");
        String initiatedBy = (String) request.get("initiatedBy");

        return ResponseEntity.ok(chargebackService.initiateChargeback(disputeId, amount, reason, initiatedBy));
    }

    @PutMapping("/{id}/notify-acquirer")
    public ResponseEntity<Chargeback> notifyAcquirer(@PathVariable UUID id) {
        return ResponseEntity.ok(chargebackService.notifyAcquirer(id));
    }

    @PutMapping("/{id}/representment")
    public ResponseEntity<Chargeback> recordRepresentment(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(chargebackService.recordRepresentment(id, body.get("evidence")));
    }

    @PutMapping("/{id}/arbitration")
    public ResponseEntity<Chargeback> escalateToArbitration(@PathVariable UUID id) {
        return ResponseEntity.ok(chargebackService.escalateToArbitration(id));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Chargeback> resolve(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(chargebackService.resolve(id, body.get("resolution"), body.get("response")));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Chargeback> reject(@PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(chargebackService.reject(id, reason));
    }

    @GetMapping("/dispute/{disputeId}")
    public ResponseEntity<Chargeback> getByDispute(@PathVariable UUID disputeId) {
        return ResponseEntity.ok(chargebackService.getByDispute(disputeId).orElse(null));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Chargeback>> getByStatus(@PathVariable String status, Pageable pageable) {
        return ResponseEntity.ok(chargebackService.getByStatus(status, pageable));
    }
}
