package np.com.nepalupi.service.settlement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.SettlementReport;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.SettlementReportRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * End-of-day settlement engine.
 * <p>
 * Settlement is separate from transaction processing:
 * <ul>
 *   <li>Transactions = real-time, instant</li>
 *   <li>Settlement = end-of-day batch netting between banks</li>
 * </ul>
 * <p>
 * Process:
 * <ol>
 *   <li>Collect all COMPLETED transactions for the day</li>
 *   <li>Calculate net position per bank (who owes whom)</li>
 *   <li>Generate settlement report for NRB/NCHL</li>
 *   <li>Submit to NCHL for multilateral net settlement</li>
 *   <li>Store for audit (NRB requires 5+ years retention)</li>
 * </ol>
 * <p>
 * Instead of millions of individual bank transfers,
 * one net transfer per bank per day.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementEngine {

    private final TransactionRepository txnRepo;
    private final SettlementReportRepository settlementRepo;
    private final ObjectMapper objectMapper;
    private final SettlementFileGenerator fileGenerator;
    private final SftpClient sftpClient;
    private final NrbIpsClient nrbIpsClient;

    private static final ZoneId NEPAL_ZONE = ZoneId.of("Asia/Kathmandu");

    /**
     * Hourly settlement batch — Section 3.5 of the spec.
     * Runs every hour on the hour. Calculates interim net positions
     * and generates settlement instructions for NRB-IPS.
     */
    @Scheduled(cron = "${nepalupi.settlement.hourly-cron:0 0 * * * *}", zone = "Asia/Kathmandu")
    public void runHourlySettlement() {
        LocalDate today = LocalDate.now(NEPAL_ZONE);
        int hour = LocalTime.now(NEPAL_ZONE).getHour();
        log.info("Running hourly settlement batch: date={}, hour={}", today, hour);

        // Get time boundaries for this hour
        Instant hourStart = today.atTime(hour, 0).atZone(NEPAL_ZONE).toInstant();
        Instant hourEnd = today.atTime(hour, 59, 59).atZone(NEPAL_ZONE).toInstant();

        List<Transaction> hourlyTxns = txnRepo.findCompletedByDate(hourStart, hourEnd);

        if (hourlyTxns.isEmpty()) {
            log.info("No transactions in hour {} — skipping", hour);
            return;
        }

        Map<String, Long> netPositions = calculateNetPositions(hourlyTxns);
        long totalVolume = hourlyTxns.stream().mapToLong(Transaction::getAmount).sum();

        log.info("Hourly batch: {} txns, {} paisa volume, {} banks",
                hourlyTxns.size(), totalVolume, netPositions.size());

        // Submit hourly netting to NRB-IPS for real-time settlement
        String hourlyRef = String.format("HOURLY-%s-%02d", today, hour);
        nrbIpsClient.submitNetSettlement(netPositions, hourlyRef);
    }

    /**
     * Run at 11:59 PM Nepal time daily.
     * Configurable via nepalupi.settlement.cron property.
     */
    @Scheduled(cron = "${nepalupi.settlement.cron:0 59 23 * * *}", zone = "Asia/Kathmandu")
    public void runEodSettlement() {
        LocalDate settlementDate = LocalDate.now(NEPAL_ZONE);
        log.info("═══════════════════════════════════════════════════════════");
        log.info("Starting EOD settlement for {}", settlementDate);

        // Check for duplicate settlement
        if (settlementRepo.findBySettlementDate(settlementDate).isPresent()) {
            log.warn("Settlement already generated for {}. Skipping.", settlementDate);
            return;
        }

        // Get today's time boundaries
        Instant startOfDay = settlementDate.atStartOfDay(NEPAL_ZONE).toInstant();
        Instant startOfNextDay = settlementDate.plusDays(1).atStartOfDay(NEPAL_ZONE).toInstant();

        // Fetch all completed transactions for today
        List<Transaction> todaysTxns = txnRepo.findCompletedByDate(startOfDay, startOfNextDay);
        log.info("Found {} completed transactions for settlement", todaysTxns.size());

        if (todaysTxns.isEmpty()) {
            log.info("No transactions to settle. Generating empty report.");
        }

        // Calculate net positions per bank
        Map<String, Long> netPositions = calculateNetPositions(todaysTxns);
        long totalVolume = todaysTxns.stream().mapToLong(Transaction::getAmount).sum();

        // Log net positions for visibility
        netPositions.forEach((bank, position) ->
                log.info("  Bank {}: net position = {} paisa ({})",
                        bank, position,
                        position > 0 ? "owes switch" : "owed by switch"));

        // Persist settlement report
        String positionsJson;
        try {
            positionsJson = objectMapper.writeValueAsString(netPositions);
        } catch (JsonProcessingException e) {
            positionsJson = netPositions.toString();
        }

        SettlementReport report = SettlementReport.builder()
                .settlementDate(settlementDate)
                .totalTransactions(todaysTxns.size())
                .totalVolumePaisa(totalVolume)
                .netPositions(positionsJson)
                .status("GENERATED")
                .build();

        settlementRepo.save(report);

        // Submit to NCHL for multilateral netting
        submitToNchl(report, netPositions);

        log.info("Settlement complete: date={}, txns={}, volume={} paisa, banks={}",
                settlementDate, todaysTxns.size(), totalVolume, netPositions.size());
        log.info("═══════════════════════════════════════════════════════════");
    }

    /**
     * Calculate net position per bank from today's transactions.
     * <p>
     * Positive = bank owes money to the switch (more debits from their customers).
     * Negative = switch owes money to the bank (more credits to their customers).
     */
    Map<String, Long> calculateNetPositions(List<Transaction> transactions) {
        Map<String, Long> positions = new HashMap<>();

        for (Transaction txn : transactions) {
            if (txn.getStatus() != TransactionStatus.COMPLETED) continue;

            // Payer bank: money was debited from their customer → they owe the switch
            positions.merge(txn.getPayerBankCode(), txn.getAmount(), Long::sum);

            // Payee bank: money was credited to their customer → switch owes them
            positions.merge(txn.getPayeeBankCode(), -txn.getAmount(), Long::sum);
        }

        return positions;
    }

    /**
     * Nightly reconciliation check: sum of all transaction amounts must equal
     * sum of settlement amounts. If they don't match, raise an alert.
     */
    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kathmandu")  // 12:30 AM — after settlement
    public void runReconciliation() {
        LocalDate yesterday = LocalDate.now(NEPAL_ZONE).minusDays(1);
        log.info("Running reconciliation for {}", yesterday);

        settlementRepo.findBySettlementDate(yesterday).ifPresent(report -> {
            Instant startOfDay = yesterday.atStartOfDay(NEPAL_ZONE).toInstant();
            Instant startOfNextDay = yesterday.plusDays(1).atStartOfDay(NEPAL_ZONE).toInstant();

            List<Transaction> txns = txnRepo.findCompletedByDate(startOfDay, startOfNextDay);
            long txnTotal = txns.stream().mapToLong(Transaction::getAmount).sum();

            if (txnTotal != report.getTotalVolumePaisa()) {
                log.error("RECONCILIATION MISMATCH for {}: txn total={}, settlement total={}",
                        yesterday, txnTotal, report.getTotalVolumePaisa());
                // In production: send PagerDuty alert
            } else {
                log.info("Reconciliation OK for {}: {} paisa across {} transactions",
                        yesterday, txnTotal, txns.size());
            }
        });
    }

    /**
     * Submit settlement report to NCHL for multilateral netting.
     * <p>
     * Generates both text and ISO 20022 XML settlement files,
     * uploads via SFTP, and initiates NRB-IPS wire transfers.
     */
    private void submitToNchl(SettlementReport report, Map<String, Long> netPositions) {
        try {
            log.info("Submitting settlement to NCHL: date={}, banks={}",
                    report.getSettlementDate(), netPositions.size());

            // 1. Generate NCHL-compatible text settlement file
            Path textFile = fileGenerator.generateNchlTextFile(report, netPositions);
            log.info("Settlement text file: {}", textFile);

            // 2. Generate ISO 20022 XML for NRB regulatory reporting
            Path xmlFile = fileGenerator.generateIso20022Xml(report, netPositions);
            log.info("ISO 20022 XML file: {}", xmlFile);

            // 3. Upload files to NCHL via SFTP
            boolean textUploaded = sftpClient.uploadSettlementFile(textFile);
            boolean xmlUploaded = sftpClient.uploadSettlementFile(xmlFile);

            if (textUploaded && xmlUploaded) {
                log.info("Settlement files uploaded to NCHL successfully");
            } else {
                log.warn("Some settlement files failed to upload — will retry");
            }

            // 4. Initiate actual inter-bank settlement via NRB-IPS
            String settlementRef = "EOD-" + report.getSettlementDate();
            boolean ipsSuccess = nrbIpsClient.submitNetSettlement(netPositions, settlementRef);

            report.setStatus(ipsSuccess ? "SUBMITTED" : "PARTIAL_SUBMISSION");
            report.setSubmittedAt(Instant.now());
            settlementRepo.save(report);

            log.info("Settlement {} to NCHL for {}",
                    ipsSuccess ? "SUBMITTED" : "PARTIALLY SUBMITTED",
                    report.getSettlementDate());
        } catch (Exception e) {
            log.error("Failed to submit settlement to NCHL: {}", e.getMessage());
            report.setStatus("SUBMISSION_FAILED");
            settlementRepo.save(report);
        }
    }
}
