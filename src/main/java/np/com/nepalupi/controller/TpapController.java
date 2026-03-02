package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.Tpap;
import np.com.nepalupi.service.psp.TpapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * TPAP Management API — Third Party Application Provider lifecycle.
 * <p>
 * TPAPs (e.g. Google Pay, PhonePe equivalents in Nepal) operate under a sponsor PSP
 * and are subject to NRB approval.
 */
@RestController
@RequestMapping("/api/v1/tpap")
@RequiredArgsConstructor
@Tag(name = "TPAP Management", description = "Third Party Application Provider registration, approval & lifecycle")
public class TpapController {

    private final TpapService tpapService;

    @PostMapping("/register")
    @Operation(summary = "Register a new TPAP", description = "TPAP must have a sponsor PSP in PRODUCTION stage")
    public ResponseEntity<Tpap> register(@RequestBody Map<String, String> request) {
        Tpap tpap = tpapService.register(
                request.get("tpapId"),
                request.get("name"),
                java.util.UUID.fromString(request.get("sponsorPspId")),
                request.get("nrbLicenseNumber"),
                request.get("nrbLicenseExpiry") != null
                        ? LocalDate.parse(request.get("nrbLicenseExpiry")) : null,
                request.get("contactEmail"),
                request.get("contactPhone")
        );
        return ResponseEntity.ok(tpap);
    }

    @PostMapping("/approve/{tpapId}")
    @Operation(summary = "NRB approves a TPAP", description = "Transitions TPAP from PENDING_APPROVAL to APPROVED")
    public ResponseEntity<Tpap> approve(@PathVariable String tpapId) {
        return ResponseEntity.ok(tpapService.approve(tpapId));
    }

    @PostMapping("/suspend/{tpapId}")
    @Operation(summary = "Suspend a TPAP", description = "Deactivates TPAP and marks as SUSPENDED")
    public ResponseEntity<Tpap> suspend(@PathVariable String tpapId) {
        return ResponseEntity.ok(tpapService.suspend(tpapId));
    }

    @GetMapping("/active")
    @Operation(summary = "List all active TPAPs")
    public ResponseEntity<List<Tpap>> listActive() {
        return ResponseEntity.ok(tpapService.listActive());
    }

    @GetMapping("/sponsor/{pspId}")
    @Operation(summary = "List TPAPs by sponsor PSP")
    public ResponseEntity<List<Tpap>> listBySponsorPsp(@PathVariable String pspId) {
        return ResponseEntity.ok(tpapService.listBySponsorPsp(pspId));
    }

    @GetMapping("/{tpapId}")
    @Operation(summary = "Get TPAP details")
    public ResponseEntity<Tpap> getByTpapId(@PathVariable String tpapId) {
        return ResponseEntity.ok(tpapService.getByTpapId(tpapId));
    }
}
