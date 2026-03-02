package np.com.nepalupi.service.compliance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.NrbMonthlyReport;
import np.com.nepalupi.repository.NrbMonthlyReportRepository;
import np.com.nepalupi.repository.PspRepository;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.repository.UserRepository;
import np.com.nepalupi.repository.VpaRepository;
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
    private final UserRepository userRepository;
    private final VpaRepository vpaRepository;
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

        // Count transactions in month using date-range query
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

        // Real VPA and user counts from repositories
        int registeredVpaCount = (int) vpaRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .count();

        int newVpaRegistrations = (int) vpaRepository.findAll().stream()
                .filter(v -> v.getCreatedAt() != null
                        && !v.getCreatedAt().isBefore(start)
                        && v.getCreatedAt().isBefore(end))
                .count();

        int activeUserCount = (int) userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null)
                .count();

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
                "{\"month\":\"" + month
                        + "\",\"totalTxns\":" + totalTxns
                        + ",\"vpas\":" + registeredVpaCount
                        + ",\"users\":" + activeUserCount + "}");

        log.info("NRB monthly report generated: month={}, txns={}, value={}, vpas={}, users={}, activePSPs={}",
                month, totalTxns, totalValue, registeredVpaCount, activeUserCount, activePspCount);

        return report;
    }

    public Optional<NrbMonthlyReport> getByMonth(YearMonth month) {
        return monthlyReportRepository.findByReportMonth(month.atDay(1));
    }
}
