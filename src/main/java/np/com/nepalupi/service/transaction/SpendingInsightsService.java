package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.SpendingInsight;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.repository.SpendingInsightRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spending insights and analytics service.
 * Provides category-wise spending analysis, trends, and budgeting data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpendingInsightsService {

    private final SpendingInsightRepository insightRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Get monthly spending summary for a user.
     */
    public Map<String, Object> getMonthlySummary(UUID userId, String yearMonth) {
        List<SpendingInsight> insights = insightRepository
                .findByUserIdAndPeriodTypeAndPeriodValue(userId, "MONTHLY", yearMonth);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("userId", userId);
        summary.put("period", yearMonth);
        summary.put("periodType", "MONTHLY");

        long totalSpent = insights.stream().mapToLong(SpendingInsight::getTotalSpentPaisa).sum();
        long totalReceived = insights.stream().mapToLong(SpendingInsight::getTotalReceivedPaisa).sum();
        int totalTxns = insights.stream().mapToInt(SpendingInsight::getTransactionCount).sum();

        summary.put("totalSpentPaisa", totalSpent);
        summary.put("totalSpentRs", totalSpent / 100.0);
        summary.put("totalReceivedPaisa", totalReceived);
        summary.put("totalReceivedRs", totalReceived / 100.0);
        summary.put("netFlowPaisa", totalReceived - totalSpent);
        summary.put("transactionCount", totalTxns);

        // Category breakdown
        List<Map<String, Object>> categoryBreakdown = insights.stream()
                .filter(i -> i.getCategory() != null)
                .map(i -> {
                    Map<String, Object> cat = new LinkedHashMap<>();
                    cat.put("category", i.getCategory());
                    cat.put("spentPaisa", i.getTotalSpentPaisa());
                    cat.put("spentRs", i.getTotalSpentPaisa() / 100.0);
                    cat.put("percentage", totalSpent > 0 ? (i.getTotalSpentPaisa() * 100.0 / totalSpent) : 0);
                    cat.put("transactionCount", i.getTransactionCount());
                    cat.put("avgAmountPaisa", i.getAvgTransactionPaisa());
                    return cat;
                })
                .sorted((a, b) -> Long.compare((long) b.get("spentPaisa"), (long) a.get("spentPaisa")))
                .toList();

        summary.put("categoryBreakdown", categoryBreakdown);
        return summary;
    }

    /**
     * Get spending trend over multiple months.
     */
    public List<Map<String, Object>> getSpendingTrend(UUID userId, int months) {
        List<SpendingInsight> allInsights = insightRepository
                .findTrendByUserAndPeriodType(userId, "MONTHLY");

        // Aggregate by period
        Map<String, List<SpendingInsight>> byPeriod = allInsights.stream()
                .collect(Collectors.groupingBy(SpendingInsight::getPeriodValue));

        return byPeriod.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(months)
                .map(e -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("period", e.getKey());
                    point.put("totalSpentPaisa", e.getValue().stream().mapToLong(SpendingInsight::getTotalSpentPaisa).sum());
                    point.put("totalReceivedPaisa", e.getValue().stream().mapToLong(SpendingInsight::getTotalReceivedPaisa).sum());
                    point.put("transactionCount", e.getValue().stream().mapToInt(SpendingInsight::getTransactionCount).sum());
                    return point;
                })
                .toList();
    }

    /**
     * Compute and store insights from raw transaction data.
     * Called by a scheduled job or on-demand.
     */
    public void computeMonthlyInsights(UUID userId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        Instant start = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = ym.atEndOfMonth().atStartOfDay(ZoneId.systemDefault())
                .plusDays(1).toInstant();

        // Delete existing for this period
        insightRepository.deleteByUserIdAndPeriodTypeAndPeriodValue(userId, "MONTHLY", yearMonth);

        // Fetch all user transactions for the month
        // Group by category and compute stats
        // Categories are determined by payee VPA patterns
        Map<String, List<Long>> categoryAmounts = new LinkedHashMap<>();

        // In production: query transactions by user and date range
        // For now, create a summary aggregate
        SpendingInsight insight = SpendingInsight.builder()
                .userId(userId)
                .periodType("MONTHLY")
                .periodValue(yearMonth)
                .category("ALL")
                .totalSpentPaisa(0L)
                .totalReceivedPaisa(0L)
                .transactionCount(0)
                .avgTransactionPaisa(0L)
                .computedAt(Instant.now())
                .build();

        insightRepository.save(insight);
        log.info("Monthly insights computed: userId={}, period={}", userId, yearMonth);
    }

    /**
     * Get top spending categories for a user.
     */
    public List<Map<String, Object>> getTopCategories(UUID userId, int limit) {
        List<SpendingInsight> insights = insightRepository
                .findByUserIdAndPeriodTypeOrderByTotalSpentPaisaDesc(userId, "MONTHLY");

        return insights.stream()
                .filter(i -> i.getCategory() != null && !"ALL".equals(i.getCategory()))
                .limit(limit)
                .map(i -> {
                    Map<String, Object> cat = new LinkedHashMap<>();
                    cat.put("category", i.getCategory());
                    cat.put("totalSpentRs", i.getTotalSpentPaisa() / 100.0);
                    cat.put("transactionCount", i.getTransactionCount());
                    return cat;
                })
                .toList();
    }

    /**
     * Categorize a transaction based on payee VPA patterns.
     */
    public String categorizeTransaction(String payeeVpa) {
        if (payeeVpa == null) return "UNCATEGORIZED";
        payeeVpa = payeeVpa.toLowerCase();

        if (payeeVpa.contains("ntc") || payeeVpa.contains("ncell") || payeeVpa.contains("recharge"))
            return "TELECOM";
        if (payeeVpa.contains("nea") || payeeVpa.contains("electricity") || payeeVpa.contains("khanepani"))
            return "UTILITIES";
        if (payeeVpa.contains("esewa") || payeeVpa.contains("khalti"))
            return "WALLET";
        if (payeeVpa.contains("hospital") || payeeVpa.contains("pharmacy") || payeeVpa.contains("medical"))
            return "HEALTH";
        if (payeeVpa.contains("school") || payeeVpa.contains("college") || payeeVpa.contains("university"))
            return "EDUCATION";
        if (payeeVpa.contains("hotel") || payeeVpa.contains("restaurant") || payeeVpa.contains("cafe"))
            return "FOOD";
        if (payeeVpa.contains("mart") || payeeVpa.contains("store") || payeeVpa.contains("shop"))
            return "SHOPPING";
        if (payeeVpa.contains("petrol") || payeeVpa.contains("fuel"))
            return "TRANSPORT";

        return "TRANSFER";
    }
}
