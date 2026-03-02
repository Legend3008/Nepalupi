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

    private static final ZoneId NEPAL_ZONE = ZoneId.of("Asia/Kathmandu");

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
     * In production: sends SFTP file / API call to NCHL settlement gateway.
     * In dev mode: logs the settlement details and marks as SUBMITTED.
     */
    private void submitToNchl(SettlementReport report, Map<String, Long> netPositions) {
        try {
            log.info("Submitting settlement to NCHL: date={}, banks={}", 
                    report.getSettlementDate(), netPositions.size());

            // Log each bank's net position for NCHL submission
            netPositions.forEach((bank, position) -> {
                String direction = position > 0 ? "PAY" : "RECEIVE";
                log.info("  NCHL settlement: bank={} direction={} amount={} paisa",
                        bank, direction, Math.abs(position));
            });

            // In production: 
            // 1. Format as NCHL settlement file (fixed-width text or ISO 20022 XML)
            // 2. Send via SFTP to NCHL gateway
            // 3. Wait for acknowledgement
            // Example: nchlGateway.submitSettlementFile(report);

            report.setStatus("SUBMITTED");
            report.setSubmittedAt(Instant.now());
            settlementRepo.save(report);

            log.info("Settlement SUBMITTED to NCHL for {}", report.getSettlementDate());
        } catch (Exception e) {
            log.error("Failed to submit settlement to NCHL: {}", e.getMessage());
            report.setStatus("SUBMISSION_FAILED");
            settlementRepo.save(report);
        }
    }
}
