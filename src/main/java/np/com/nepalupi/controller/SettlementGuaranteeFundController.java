package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.SettlementGuaranteeFund;
import np.com.nepalupi.service.settlement.SettlementGuaranteeFundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Settlement Guarantee Fund API — NRB-mandated fund management.
 */
@RestController
@RequestMapping("/api/v1/sgf")
@RequiredArgsConstructor
@Tag(name = "Settlement Guarantee Fund", description = "NRB-mandated fund for inter-bank UPI settlement guarantee")
public class SettlementGuaranteeFundController {

    private final SettlementGuaranteeFundService sgfService;

    @PostMapping("/contribute")
    @Operation(summary = "Record bank contribution", description = "Bank contributes to the SGF — requires NRB approval")
    public ResponseEntity<SettlementGuaranteeFund> contribute(@RequestBody Map<String, Object> request) {
        SettlementGuaranteeFund sgf = sgfService.recordContribution(
                (String) request.get("bankCode"),
                ((Number) request.get("contributionPaisa")).longValue()
        );
        return ResponseEntity.ok(sgf);
    }

    @PostMapping("/utilize")
    @Operation(summary = "Utilize fund for settlement default", description = "Covers settlement default — requires NRB approval")
    public ResponseEntity<SettlementGuaranteeFund> utilize(@RequestBody Map<String, Object> request) {
        SettlementGuaranteeFund sgf = sgfService.utilizeFund(
                (String) request.get("bankCode"),
                ((Number) request.get("amountPaisa")).longValue()
        );
        return ResponseEntity.ok(sgf);
    }

    @PostMapping("/replenish")
    @Operation(summary = "Replenish fund after utilization")
    public ResponseEntity<SettlementGuaranteeFund> replenish(@RequestBody Map<String, Object> request) {
        SettlementGuaranteeFund sgf = sgfService.replenishFund(
                (String) request.get("bankCode"),
                ((Number) request.get("replenishmentPaisa")).longValue()
        );
        return ResponseEntity.ok(sgf);
    }

    @PostMapping("/approve/{sgfId}")
    @Operation(summary = "NRB approves SGF record")
    public ResponseEntity<SettlementGuaranteeFund> approve(@PathVariable UUID sgfId) {
        return ResponseEntity.ok(sgfService.approveByNrb(sgfId));
    }

    @GetMapping("/balance/{bankCode}")
    @Operation(summary = "Get available fund for a bank")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String bankCode) {
        Long balance = sgfService.getAvailableFund(bankCode);
        return ResponseEntity.ok(Map.of(
                "bankCode", bankCode,
                "availableFundPaisa", balance,
                "availableFundNpr", balance / 100.0
        ));
    }

    @GetMapping("/total")
    @Operation(summary = "Get total available fund across all banks")
    public ResponseEntity<Map<String, Object>> getTotalBalance() {
        Long total = sgfService.getTotalAvailableFund();
        return ResponseEntity.ok(Map.of(
                "totalAvailableFundPaisa", total,
                "totalAvailableFundNpr", total / 100.0
        ));
    }

    @GetMapping("/history/{bankCode}")
    @Operation(summary = "Get SGF history for a bank")
    public ResponseEntity<List<SettlementGuaranteeFund>> getHistory(@PathVariable String bankCode) {
        return ResponseEntity.ok(sgfService.getHistoryByBank(bankCode));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending NRB approvals")
    public ResponseEntity<List<SettlementGuaranteeFund>> getPendingApprovals() {
        return ResponseEntity.ok(sgfService.getPendingApprovals());
    }
}
