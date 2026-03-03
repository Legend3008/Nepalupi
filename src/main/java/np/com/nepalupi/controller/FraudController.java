package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.FraudFlag;
import np.com.nepalupi.service.fraud.AccountFreezeService;
import np.com.nepalupi.service.fraud.FraudReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Fraud review and account freeze management API.
 */
@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudReviewService fraudReviewService;
    private final AccountFreezeService accountFreezeService;

    // ── Fraud Flag Review ──

    @GetMapping("/flags/unreviewed")
    public ResponseEntity<Page<FraudFlag>> getUnreviewedFlags(Pageable pageable) {
        return ResponseEntity.ok(fraudReviewService.getUnreviewedFlags(pageable));
    }

    @GetMapping("/flags/user/{userId}")
    public ResponseEntity<List<FraudFlag>> getFlagsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(fraudReviewService.getFlagsByUser(userId));
    }

    @GetMapping("/flags/transaction/{transactionId}")
    public ResponseEntity<List<FraudFlag>> getFlagsByTransaction(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(fraudReviewService.getFlagsByTransaction(transactionId));
    }

    @PutMapping("/flags/{flagId}/confirm")
    public ResponseEntity<FraudFlag> confirmFraud(@PathVariable UUID flagId,
                                                   @RequestParam String reviewerId) {
        return ResponseEntity.ok(fraudReviewService.confirmFraud(flagId, reviewerId));
    }

    @PutMapping("/flags/{flagId}/dismiss")
    public ResponseEntity<FraudFlag> dismissFlag(@PathVariable UUID flagId,
                                                  @RequestParam String reviewerId) {
        return ResponseEntity.ok(fraudReviewService.dismissFlag(flagId, reviewerId));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getReviewDashboard() {
        return ResponseEntity.ok(fraudReviewService.getReviewDashboard());
    }

    // ── Account Freeze Management ──

    @PostMapping("/freeze/{userId}")
    public ResponseEntity<Map<String, Object>> freezeAccount(@PathVariable UUID userId,
                                                              @RequestParam String reason) {
        accountFreezeService.freezeAccount(userId, reason);
        return ResponseEntity.ok(Map.of("userId", userId, "status", "FROZEN", "reason", reason));
    }

    @PostMapping("/unfreeze/{userId}")
    public ResponseEntity<Map<String, Object>> unfreezeAccount(@PathVariable UUID userId,
                                                                @RequestParam String adminId,
                                                                @RequestParam String reason) {
        accountFreezeService.unfreezeAccount(userId, adminId, reason);
        return ResponseEntity.ok(Map.of("userId", userId, "status", "UNFROZEN"));
    }

    @GetMapping("/freeze/{userId}/status")
    public ResponseEntity<Map<String, Object>> getFreezeStatus(@PathVariable UUID userId) {
        boolean frozen = accountFreezeService.isFrozen(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "isFrozen", frozen));
    }
}
