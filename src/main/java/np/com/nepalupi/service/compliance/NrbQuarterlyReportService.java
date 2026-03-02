package np.com.nepalupi.service.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.NrbQuarterlyReport;
import np.com.nepalupi.repository.NrbQuarterlyReportRepository;
import np.com.nepalupi.repository.FraudFlagRepository;
import np.com.nepalupi.repository.SuspiciousTransactionReportRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

/**
 * Generates NRB quarterly risk & fraud reports.
 * Scheduled on the 5th day of each quarter-start month (Jan, Apr, Jul, Oct) at 6 AM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NrbQuarterlyReportService {

    private static final ZoneId NPT = ZoneId.of("Asia/Kathmandu");

    private final NrbQuarterlyReportRepository quarterlyReportRepository;
    private final SuspiciousTransactionReportRepository strRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final TransactionRepository transactionRepository;
    private final ComplianceAuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * Generate quarterly report on the 5th of Jan, Apr, Jul, Oct at 6 AM.
     */
    @Scheduled(cron = "0 0 6 5 1,4,7,10 *", zone = "Asia/Kathmandu")
    @Transactional
    public void generateQuarterlyReport() {
        LocalDate now = LocalDate.now(NPT);
        String quarter = getQuarterLabel(now.minusMonths(1)); // previous quarter
        log.info("Generating NRB quarterly report for {}", quarter);
        generateForQuarter(quarter);
    }

    @Transactional
    public NrbQuarterlyReport generateForQuarter(String quarter) {
        // Parse quarter range
        int year = Integer.parseInt(quarter.substring(0, 4));
        int q = Integer.parseInt(quarter.substring(6));
        LocalDate qStart = LocalDate.of(year, (q - 1) * 3 + 1, 1);
        LocalDate qEnd = qStart.plusMonths(3).minusDays(1);

        Instant startInstant = qStart.atStartOfDay(NPT).toInstant();
        Instant endInstant = qEnd.plusDays(1).atStartOfDay(NPT).toInstant();

        // Count STRs filed in this quarter
        long strCount = strRepository.countByFiledWithFiuTrueAndCreatedAtBetween(startInstant, endInstant);
        long totalStrCount = strRepository.countByCreatedAtBetween(startInstant, endInstant);

        NrbQuarterlyReport report = quarterlyReportRepository.findByReportQuarter(quarter)
                .orElse(NrbQuarterlyReport.builder().reportQuarter(quarter).build());

        report.setStrFiledCount((int) strCount);

        // Real fraud data from fraud flags and transactions
        long fraudFlagCount = fraudFlagRepository.findAll().stream()
                .filter(f -> f.getCreatedAt() != null
                        && !f.getCreatedAt().isBefore(startInstant)
                        && f.getCreatedAt().isBefore(endInstant))
                .count();

        long fraudValue = fraudFlagRepository.findAll().stream()
                .filter(f -> f.getCreatedAt() != null
                        && !f.getCreatedAt().isBefore(startInstant)
                        && f.getCreatedAt().isBefore(endInstant)
                        && f.getTransactionId() != null)
                .mapToLong(f -> transactionRepository.findById(f.getTransactionId())
                        .map(t -> t.getAmount() != null ? t.getAmount() : 0L)
                        .orElse(0L))
                .sum();

        report.setFraudIncidentCount((int) fraudFlagCount);
        report.setFraudTotalValuePaisa(fraudValue);
        report.setFraudTypes(toJson(Map.of(
                "AMOUNT_SPIKE", fraudFlagRepository.findAll().stream()
                        .filter(f -> f.getSignals() != null && f.getSignals().contains("AMOUNT_SPIKE")).count(),
                "HIGH_VELOCITY", fraudFlagRepository.findAll().stream()
                        .filter(f -> f.getSignals() != null && f.getSignals().contains("HIGH_VELOCITY")).count()
        )));
        report.setFraudResolutionSummary(toJson(Map.of(
                "reviewed", fraudFlagRepository.findAll().stream()
                        .filter(f -> Boolean.TRUE.equals(f.getReviewed())).count(),
                "pending", fraudFlagRepository.findAll().stream()
                        .filter(f -> !Boolean.TRUE.equals(f.getReviewed())).count()
        )));
        report.setSystemDowntimeMinutes(0); // Would integrate with incident tracking
        report.setDowntimeIncidents("[]");
        report.setSecurityIncidents(0);

        report = quarterlyReportRepository.save(report);

        auditService.recordEvent("QUARTERLY_REPORT_GENERATED", "NRB_REPORT",
                report.getId().toString(),
                "{\"quarter\":\"" + quarter + "\",\"strFiled\":" + strCount + "}");

        log.info("NRB quarterly report generated: quarter={}, STRs={}", quarter, strCount);
        return report;
    }

    public Optional<NrbQuarterlyReport> getByQuarter(String quarter) {
        return quarterlyReportRepository.findByReportQuarter(quarter);
    }

    private String getQuarterLabel(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return date.getYear() + "-Q" + quarter;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
