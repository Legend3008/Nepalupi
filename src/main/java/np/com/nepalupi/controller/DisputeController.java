package np.com.nepalupi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.dto.request.DisputeRequest;
import np.com.nepalupi.domain.entity.Dispute;
import np.com.nepalupi.domain.entity.DisputeActionLog;
import np.com.nepalupi.service.dispute.DisputeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dispute API — full lifecycle management for transaction disputes.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>POST /raise — raise a new dispute</li>
 *   <li>GET /{id} — get dispute by ID</li>
 *   <li>GET /case/{caseRef} — get dispute by case reference</li>
 *   <li>GET /{id}/trail — get full action audit trail</li>
 *   <li>POST /{id}/acknowledge — acknowledge a dispute</li>
 *   <li>POST /{id}/bank-query — send bank query</li>
 *   <li>POST /{id}/bank-response — record bank response</li>
 *   <li>POST /{id}/resolve — resolve dispute</li>
 *   <li>POST /{id}/close — close dispute</li>
 *   <li>POST /{id}/escalate — escalate to NCHL/NRB</li>
 *   <li>GET /sla-breaches — list all SLA-breached disputes</li>
 *   <li>GET /by-vpa/{vpa} — list disputes by user VPA</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/dispute")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping("/raise")
    public ResponseEntity<Dispute> raiseDispute(@Valid @RequestBody DisputeRequest request) {
        Dispute dispute = disputeService.raiseDispute(
                request.getTransactionId(),
                request.getRaisedByVpa(),
                request.getReason(),
                request.getDisputeType()
        );
        return ResponseEntity.status(201).body(dispute);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dispute> getDispute(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.getDispute(id));
    }

    @GetMapping("/case/{caseRef}")
    public ResponseEntity<Dispute> getDisputeByCaseRef(@PathVariable String caseRef) {
        return ResponseEntity.ok(disputeService.getDisputeByCaseRef(caseRef));
    }

    @GetMapping("/{id}/trail")
    public ResponseEntity<List<DisputeActionLog>> getActionTrail(@PathVariable UUID id) {
        return ResponseEntity.ok(disputeService.getActionTrail(id));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Dispute> acknowledge(@PathVariable UUID id,
                                                @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(disputeService.acknowledge(id,
                body.getOrDefault("acknowledgedBy", "ops-agent")));
    }

    @PostMapping("/{id}/bank-query")
    public ResponseEntity<Dispute> sendBankQuery(@PathVariable UUID id,
                                                  @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(disputeService.sendBankQuery(id,
                body.get("bankCode"),
                body.get("queryDetails"),
                body.getOrDefault("sentBy", "ops-agent")));
    }

    @PostMapping("/{id}/bank-response")
    public ResponseEntity<Dispute> recordBankResponse(@PathVariable UUID id,
                                                       @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(disputeService.recordBankResponse(id,
                body.get("responseDetails"),
                body.getOrDefault("recordedBy", "ops-agent")));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Dispute> resolve(@PathVariable UUID id,
                                            @RequestBody Map<String, String> body) {
        UUID refundTxnId = body.containsKey("refundTxnId") ? UUID.fromString(body.get("refundTxnId")) : null;
        return ResponseEntity.ok(disputeService.resolve(id,
                body.get("resolution"), refundTxnId,
                body.getOrDefault("resolvedBy", "ops-agent")));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Dispute> closeDispute(@PathVariable UUID id,
                                                 @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(disputeService.closeDispute(id,
                body.getOrDefault("closedBy", "ops-agent")));
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<Dispute> escalate(@PathVariable UUID id,
                                             @RequestBody Map<String, String> body) {
        int level = Integer.parseInt(body.getOrDefault("level", "1"));
        return ResponseEntity.ok(disputeService.escalate(id, level,
                body.getOrDefault("escalatedBy", "system"),
                body.get("reason")));
    }

    @GetMapping("/sla-breaches")
    public ResponseEntity<List<Dispute>> getSlaBreaches() {
        return ResponseEntity.ok(disputeService.getSlaBreaches());
    }

    @GetMapping("/by-vpa/{vpa}")
    public ResponseEntity<List<Dispute>> getDisputesByVpa(@PathVariable String vpa) {
        return ResponseEntity.ok(disputeService.getDisputesByVpa(vpa));
    }
}
