package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.service.transaction.SpendingInsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Spending analytics and insights API.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final SpendingInsightsService insightsService;

    @GetMapping("/monthly/{userId}")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(@PathVariable UUID userId,
                                                                  @RequestParam String yearMonth) {
        return ResponseEntity.ok(insightsService.getMonthlySummary(userId, yearMonth));
    }

    @GetMapping("/trend/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getSpendingTrend(@PathVariable UUID userId,
                                                                       @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(insightsService.getSpendingTrend(userId, months));
    }

    @GetMapping("/categories/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTopCategories(@PathVariable UUID userId,
                                                                       @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(insightsService.getTopCategories(userId, limit));
    }

    @PostMapping("/compute/{userId}")
    public ResponseEntity<Map<String, String>> computeInsights(@PathVariable UUID userId,
                                                                @RequestParam String yearMonth) {
        insightsService.computeMonthlyInsights(userId, yearMonth);
        return ResponseEntity.ok(Map.of("status", "COMPUTED", "userId", userId.toString(), "period", yearMonth));
    }

    @GetMapping("/categorize")
    public ResponseEntity<Map<String, String>> categorizeVpa(@RequestParam String payeeVpa) {
        String category = insightsService.categorizeTransaction(payeeVpa);
        return ResponseEntity.ok(Map.of("payeeVpa", payeeVpa, "category", category));
    }
}
