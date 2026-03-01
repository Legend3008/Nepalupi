package np.com.nepalupi.service.iso8583;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Iso8583MessageLog;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.Iso8583MessageLogRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Daily reconciliation between our transaction records and NCHL ISO 8583 message log.
 * <p>
 * Runs at 2:00 AM Nepal time every day.
 * Compares every COMPLETED transaction in our DB against the corresponding
 * 0210 response messages we received from NCHL.
 * <p>
 * Any discrepancy = reconciliation break → alert ops team, NRB will ask about these.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NchlReconciliationService {

    private final TransactionRepository txnRepo;
    private final Iso8583MessageLogRepository messageLogRepo;

    private static final ZoneId NEPAL_ZONE = ZoneId.of("Asia/Kathmandu");

    /**
     * Daily reconciliation job at 2:00 AM Nepal time.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kathmandu")
    public void runDailyReconciliation() {
        LocalDate yesterday = LocalDate.now(NEPAL_ZONE).minusDays(1);
        log.info("═══════════════════════════════════════════════════════════");
        log.info("Starting NCHL Reconciliation for {}", yesterday);

        ReconciliationResult result = reconcile(yesterday);

        log.info("Reconciliation complete: matched={}, our_only={}, nchl_only={}, amount_mismatch={}",
                result.matched, result.ourOnly.size(), result.nchlOnly.size(), result.amountMismatches.size());

        if (!result.ourOnly.isEmpty() || !result.nchlOnly.isEmpty() || !result.amountMismatches.isEmpty()) {
            log.error("═══ RECONCILIATION BREAKS FOUND — REQUIRES INVESTIGATION ═══");
            result.ourOnly.forEach(rrn -> log.error("  BREAK: RRN {} in our DB but NOT in NCHL messages", rrn));
            result.nchlOnly.forEach(rrn -> log.error("  BREAK: RRN {} in NCHL messages but NOT in our DB", rrn));
            result.amountMismatches.forEach((rrn, detail) -> log.error("  BREAK: RRN {} amount mismatch — {}", rrn, detail));
            // In production: send alert to ops team, create JIRA ticket, etc.
        }

        log.info("═══════════════════════════════════════════════════════════");
    }

    /**
     * Reconcile transactions for a given date.
     */
    public ReconciliationResult reconcile(LocalDate date) {
        Instant startOfDay = date.atStartOfDay(NEPAL_ZONE).toInstant();
        Instant startOfNextDay = date.plusDays(1).atStartOfDay(NEPAL_ZONE).toInstant();

        // Our completed transactions
        List<Transaction> ourTxns = txnRepo.findCompletedByDate(startOfDay, startOfNextDay);
        Map<String, Transaction> ourByRrn = ourTxns.stream()
                .collect(Collectors.toMap(Transaction::getRrn, t -> t, (a, b) -> a));

        // NCHL approved responses (0210 with response code 00)
        List<Iso8583MessageLog> nchlResponses = messageLogRepo.findByDateRange(startOfDay, startOfNextDay)
                .stream()
                .filter(m -> "INBOUND".equals(m.getDirection()))
                .filter(m -> "0210".equals(m.getMti()))
                .filter(m -> "00".equals(m.getResponseCode()))
                .toList();
        Map<String, Iso8583MessageLog> nchlByRrn = nchlResponses.stream()
                .filter(m -> m.getRrn() != null)
                .collect(Collectors.toMap(Iso8583MessageLog::getRrn, m -> m, (a, b) -> a));

        // ── Compare ──
        ReconciliationResult result = new ReconciliationResult();

        // Check our transactions against NCHL
        for (Map.Entry<String, Transaction> entry : ourByRrn.entrySet()) {
            String rrn = entry.getKey();
            Transaction ourTxn = entry.getValue();

            Iso8583MessageLog nchlMsg = nchlByRrn.get(rrn);
            if (nchlMsg == null) {
                result.ourOnly.add(rrn);
            } else if (nchlMsg.getAmountPaisa() != null && !nchlMsg.getAmountPaisa().equals(ourTxn.getAmount())) {
                result.amountMismatches.put(rrn,
                        "our=" + ourTxn.getAmount() + " nchl=" + nchlMsg.getAmountPaisa());
            } else {
                result.matched++;
            }
        }

        // Check NCHL messages not in our DB
        for (String rrn : nchlByRrn.keySet()) {
            if (!ourByRrn.containsKey(rrn)) {
                result.nchlOnly.add(rrn);
            }
        }

        return result;
    }

    /**
     * Result of a reconciliation run.
     */
    public static class ReconciliationResult {
        public int matched = 0;
        public List<String> ourOnly = new ArrayList<>();      // in our DB, not in NCHL
        public List<String> nchlOnly = new ArrayList<>();     // in NCHL, not in our DB
        public Map<String, String> amountMismatches = new HashMap<>();

        public boolean hasBreaks() {
            return !ourOnly.isEmpty() || !nchlOnly.isEmpty() || !amountMismatches.isEmpty();
        }
    }
}
