package np.com.nepalupi.service.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.NrbDailyReport;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.NrbDailyReportRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Generates NRB daily transaction reports.
 * Scheduled at 9 AM NPT for the previous day.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NrbDailyReportService {

    private static final ZoneId NPT = ZoneId.of("Asia/Kathmandu");

    private final NrbDailyReportRepository dailyReportRepository;
    private final TransactionRepository transactionRepository;
    private final ComplianceAuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Generate daily report at 9 AM NPT for previous day.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kathmandu")
    @Transactional
    public void generateDailyReport() {
        LocalDate yesterday = LocalDate.now(NPT).minusDays(1);
        log.info("Generating NRB daily report for {}", yesterday);

        try {
            NrbDailyReport report = generateForDate(yesterday);
            auditService.recordEvent("DAILY_REPORT_GENERATED", "NRB_REPORT",
                    report.getId().toString(),
                    "{\"date\":\"" + yesterday + "\",\"totalTxns\":" + report.getTotalTxnCount() + "}");
        } catch (Exception e) {
            log.error("Failed to generate daily report for {}: {}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * Generate or refresh report for a specific date.
     */
    @Transactional
    public NrbDailyReport generateForDate(LocalDate date) {
        Instant dayStart = date.atStartOfDay(NPT).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(NPT).toInstant();

        List<Transaction> dayTransactions = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(dayStart)
                        && t.getCreatedAt().isBefore(dayEnd))
                .toList();

        int totalCount = dayTransactions.size();
        long totalValue = dayTransactions.stream()
                .mapToLong(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();

        int successCount = (int) dayTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .count();
        int failureCount = (int) dayTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.DEBIT_FAILED
                        || t.getStatus() == TransactionStatus.CREDIT_FAILED)
                .count();

        // Count reversals
        int reversalCount = (int) dayTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.REVERSED)
                .count();
        long reversalValue = dayTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.REVERSED)
                .mapToLong(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();

        // Failure reasons breakdown
        Map<String, Integer> failureReasons = new HashMap<>();
        dayTransactions.stream()
                .filter(t -> (t.getStatus() == TransactionStatus.DEBIT_FAILED
                        || t.getStatus() == TransactionStatus.CREDIT_FAILED)
                        && t.getFailureReason() != null)
                .forEach(t -> failureReasons.merge(t.getFailureReason(), 1, Integer::sum));

        String failureReasonsJson = toJson(failureReasons);

        NrbDailyReport report = dailyReportRepository.findByReportDate(date)
                .orElse(NrbDailyReport.builder().reportDate(date).build());

        report.setTotalTxnCount(totalCount);
        report.setTotalTxnValuePaisa(totalValue);
        report.setSuccessCount(successCount);
        report.setFailureCount(failureCount);
        report.setReversalCount(reversalCount);
        report.setReversalValuePaisa(reversalValue);
        report.setFailureReasons(failureReasonsJson);
        // P2P = all for now (P2M not yet implemented)
        report.setP2pCount(successCount);
        report.setP2pValuePaisa(totalValue);

        report = dailyReportRepository.save(report);
        log.info("NRB daily report generated: date={}, txns={}, value={}",
                date, totalCount, totalValue);

        return report;
    }

    /**
     * Mark a report as submitted to NRB.
     */
    @Transactional
    public NrbDailyReport markSubmitted(UUID reportId) {
        NrbDailyReport report = dailyReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.setSubmitted(true);
        report.setSubmittedAt(Instant.now());
        report = dailyReportRepository.save(report);

        auditService.recordEvent("DAILY_REPORT_SUBMITTED", "NRB_REPORT",
                reportId.toString(), "{\"date\":\"" + report.getReportDate() + "\"}");

        log.info("NRB daily report submitted: {}", report.getReportDate());
        return report;
    }

    /**
     * Get report by date.
     */
    public Optional<NrbDailyReport> getByDate(LocalDate date) {
        return dailyReportRepository.findByReportDate(date);
    }

    /**
     * Get reports in a date range.
     */
    public List<NrbDailyReport> getByDateRange(LocalDate start, LocalDate end) {
        return dailyReportRepository.findByReportDateBetweenOrderByReportDateDesc(start, end);
    }

    /**
     * Get all un-submitted reports.
     */
    public List<NrbDailyReport> getPendingSubmission() {
        return dailyReportRepository.findBySubmittedFalseOrderByReportDateAsc();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
