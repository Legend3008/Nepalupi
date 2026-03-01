package np.com.nepalupi.service.compliance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.NrbMonthlyReport;
import np.com.nepalupi.repository.NrbMonthlyReportRepository;
import np.com.nepalupi.repository.PspRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Generates NRB monthly volume reports — user counts, VPA registrations, PSP activity.
 * Scheduled on the 2nd of each month at 6 AM NPT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NrbMonthlyReportService {

    private static final ZoneId NPT = ZoneId.of("Asia/Kathmandu");

    private final NrbMonthlyReportRepository monthlyReportRepository;
    private final TransactionRepository transactionRepository;
    private final PspRepository pspRepository;
    private final ComplianceAuditService auditService;

    @Scheduled(cron = "0 0 6 2 * *", zone = "Asia/Kathmandu")
    @Transactional
    public void generateMonthlyReport() {
        YearMonth lastMonth = YearMonth.now(NPT).minusMonths(1);
        log.info("Generating NRB monthly report for {}", lastMonth);
        generateForMonth(lastMonth);
    }

    @Transactional
    public NrbMonthlyReport generateForMonth(YearMonth month) {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        Instant start = monthStart.atStartOfDay(NPT).toInstant();
        Instant end = monthEnd.plusDays(1).atStartOfDay(NPT).toInstant();

        // Count transactions in month
        long totalTxns = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(start)
                        && t.getCreatedAt().isBefore(end))
                .count();

        long totalValue = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt() != null
                        && !t.getCreatedAt().isBefore(start)
                        && t.getCreatedAt().isBefore(end))
                .mapToLong(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();

        // Count active PSPs
        int activePspCount = (int) pspRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .count();

        // Mock VPA counts — real system would query VPA repository
        int registeredVpaCount = 0;
        int newVpaRegistrations = 0;
        int activeUserCount = 0;

        NrbMonthlyReport report = monthlyReportRepository.findByReportMonth(monthStart)
                .orElse(NrbMonthlyReport.builder().reportMonth(monthStart).build());

        report.setTotalTxnCount((int) totalTxns);
        report.setTotalTxnValuePaisa(totalValue);
        report.setActivePspCount(activePspCount);
        report.setRegisteredVpaCount(registeredVpaCount);
        report.setNewVpaRegistrations(newVpaRegistrations);
        report.setActiveUserCount(activeUserCount);

        report = monthlyReportRepository.save(report);

        auditService.recordEvent("MONTHLY_REPORT_GENERATED", "NRB_REPORT",
                report.getId().toString(),
                "{\"month\":\"" + month + "\",\"totalTxns\":" + totalTxns + "}");

        log.info("NRB monthly report generated: month={}, txns={}, activePSPs={}",
                month, totalTxns, activePspCount);

        return report;
    }

    public Optional<NrbMonthlyReport> getByMonth(YearMonth month) {
        return monthlyReportRepository.findByReportMonth(month.atDay(1));
    }
}
