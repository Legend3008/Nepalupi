package np.com.nepalupi.service.psp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.entity.Psp;
import np.com.nepalupi.domain.entity.PspHealthReport;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.PspHealthReportRepository;
import np.com.nepalupi.repository.PspRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Generates monthly PSP health reports — uptime, success rate, avg latency, volume.
 * Scheduled to run on the 1st of every month at 3 AM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspHealthReportService {

    private final PspHealthReportRepository healthReportRepository;
    private final PspRepository pspRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Generate monthly health report for all active PSPs.
     * Runs at 3 AM on the 1st of every month.
     */
    @Scheduled(cron = "0 0 3 1 * *")
    @Transactional
    public void generateMonthlyReports() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        log.info("Generating PSP health reports for {}", lastMonth);

        List<Psp> activePsps = pspRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .toList();

        for (Psp psp : activePsps) {
            try {
                generateReport(psp.getId(), lastMonth);
            } catch (Exception e) {
                log.error("Failed to generate health report for PSP {}: {}", psp.getPspId(), e.getMessage());
            }
        }

        log.info("Generated {} PSP health reports for {}", activePsps.size(), lastMonth);
    }

    /**
     * Generate or refresh a health report for a specific PSP and month.
     */
    @Transactional
    public PspHealthReport generateReport(UUID pspId, YearMonth month) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // Get all transactions for the PSP in the date range
        Instant startInstant = start.atStartOfDay(ZoneId.of("Asia/Kathmandu")).toInstant();
        Instant endInstant = end.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kathmandu")).toInstant();
        List<Transaction> pspTxns = transactionRepository.findAll().stream()
                .filter(t -> psp.getPspId().equals(t.getPspId()))
                .filter(t -> t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(startInstant)
                        && t.getCreatedAt().isBefore(endInstant))
                .toList();

        long totalTxns = pspTxns.size();
        long successfulTxns = pspTxns.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .count();
        long failedTxns = pspTxns.stream()
                .filter(t -> t.getStatus() == TransactionStatus.DEBIT_FAILED
                        || t.getStatus() == TransactionStatus.CREDIT_FAILED)
                .count();

        BigDecimal successRate = totalTxns > 0
                ? BigDecimal.valueOf(successfulTxns)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalTxns), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate real average response time from completed transactions
        int avgResponseMs = (int) pspTxns.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED
                        && t.getInitiatedAt() != null && t.getCompletedAt() != null)
                .mapToLong(t -> t.getCompletedAt().toEpochMilli() - t.getInitiatedAt().toEpochMilli())
                .average()
                .orElse(0);

        long totalVolume = pspTxns.stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .mapToLong(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();

        PspHealthReport report = healthReportRepository
                .findByPspIdAndReportMonth(psp.getPspId(), start)
                .orElse(PspHealthReport.builder()
                        .pspId(psp.getPspId())
                        .reportMonth(start)
                        .build());

        report.setTotalTransactions((int) totalTxns);
        report.setSuccessfulTxns((int) successfulTxns);
        report.setFailedTxns((int) failedTxns);
        report.setSuccessRate(successRate);
        report.setAvgResponseMs(avgResponseMs);
        report.setTotalVolumePaisa(totalVolume);

        report = healthReportRepository.save(report);
        log.info("Health report generated for PSP {} month {}: txns={}, success={}%",
                psp.getPspId(), month, totalTxns, successRate);

        return report;
    }

    /**
     * Get all health reports for a PSP.
     */
    public List<PspHealthReport> getReports(UUID pspId) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));
        return healthReportRepository.findByPspIdOrderByReportMonthDesc(psp.getPspId());
    }

    /**
     * Get health reports for all PSPs for a given month.
     */
    public List<PspHealthReport> getReportsByMonth(YearMonth month) {
        return healthReportRepository.findByReportMonth(month.atDay(1));
    }

}
