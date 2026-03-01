package np.com.nepalupi.launch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.launch.entity.LaunchMetric;
import np.com.nepalupi.launch.repository.LaunchMetricRepository;
import np.com.nepalupi.launch.repository.MerchantAcquisitionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaunchMetricsService {

    private final LaunchMetricRepository launchMetricRepository;
    private final MerchantAcquisitionRepository merchantAcquisitionRepository;

    // ─── Metric Recording ───────────────────────────────────────────

    @Transactional
    public LaunchMetric recordDailyMetrics(LaunchMetric metric) {
        metric.setMetricDate(LocalDate.now());
        log.info("Recording daily launch metrics for {}", metric.getMetricDate());
        return launchMetricRepository.save(metric);
    }

    @Transactional
    public LaunchMetric updateDailyMetrics(LocalDate date, LaunchMetric updates) {
        LaunchMetric existing = launchMetricRepository.findByMetricDate(date)
                .orElseGet(() -> {
                    LaunchMetric newMetric = new LaunchMetric();
                    newMetric.setMetricDate(date);
                    return newMetric;
                });

        if (updates.getTotalRegisteredUsers() != null) existing.setTotalRegisteredUsers(updates.getTotalRegisteredUsers());
        if (updates.getNewRegistrationsToday() != null) existing.setNewRegistrationsToday(updates.getNewRegistrationsToday());
        if (updates.getActiveUsers30d() != null) existing.setActiveUsers30d(updates.getActiveUsers30d());
        if (updates.getTotalTransactionsToday() != null) existing.setTotalTransactionsToday(updates.getTotalTransactionsToday());
        if (updates.getTotalVolumePaisaToday() != null) existing.setTotalVolumePaisaToday(updates.getTotalVolumePaisaToday());
        if (updates.getTxnSuccessRatePct() != null) existing.setTxnSuccessRatePct(updates.getTxnSuccessRatePct());
        if (updates.getAvgTxnAmountPaisa() != null) existing.setAvgTxnAmountPaisa(updates.getAvgTxnAmountPaisa());
        if (updates.getP2pCount() != null) existing.setP2pCount(updates.getP2pCount());
        if (updates.getP2mCount() != null) existing.setP2mCount(updates.getP2mCount());
        if (updates.getTotalActiveMerchants() != null) existing.setTotalActiveMerchants(updates.getTotalActiveMerchants());
        if (updates.getNewMerchantsToday() != null) existing.setNewMerchantsToday(updates.getNewMerchantsToday());
        if (updates.getTotalBanksLive() != null) existing.setTotalBanksLive(updates.getTotalBanksLive());
        if (updates.getBankingCoveragePct() != null) existing.setBankingCoveragePct(updates.getBankingCoveragePct());
        if (updates.getSettlementAccuracyPct() != null) existing.setSettlementAccuracyPct(updates.getSettlementAccuracyPct());
        if (updates.getReconciliationBreaks() != null) existing.setReconciliationBreaks(updates.getReconciliationBreaks());
        if (updates.getTotalPspAppsLive() != null) existing.setTotalPspAppsLive(updates.getTotalPspAppsLive());

        return launchMetricRepository.save(existing);
    }

    // ─── Metric Retrieval ───────────────────────────────────────────

    public Optional<LaunchMetric> getLatestMetrics() {
        return launchMetricRepository.findLatest();
    }

    public List<LaunchMetric> getMetricsRange(LocalDate start, LocalDate end) {
        return launchMetricRepository.findByMetricDateBetweenOrderByMetricDateAsc(start, end);
    }

    public List<LaunchMetric> getRecentMetrics(int days) {
        return launchMetricRepository.findRecentDays(days);
    }

    // ─── Dashboard Summary ──────────────────────────────────────────

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        Optional<LaunchMetric> latest = launchMetricRepository.findLatest();
        latest.ifPresent(m -> {
            summary.put("date", m.getMetricDate());
            summary.put("totalUsers", m.getTotalRegisteredUsers());
            summary.put("newRegistrations", m.getNewRegistrationsToday());
            summary.put("activeUsers30d", m.getActiveUsers30d());
            summary.put("totalTransactions", m.getTotalTransactionsToday());
            summary.put("totalVolumePaisa", m.getTotalVolumePaisaToday());
            summary.put("p2pCount", m.getP2pCount());
            summary.put("p2mCount", m.getP2mCount());
            summary.put("activeMerchants", m.getTotalActiveMerchants());
            summary.put("banksLive", m.getTotalBanksLive());
            summary.put("pspAppsLive", m.getTotalPspAppsLive());
            summary.put("txnSuccessRate", m.getTxnSuccessRatePct());
            summary.put("settlementAccuracy", m.getSettlementAccuracyPct());
            summary.put("reconciliationBreaks", m.getReconciliationBreaks());
        });

        summary.put("totalAcquiredMerchants", merchantAcquisitionRepository.countActiveMerchants());
        summary.put("merchantsByCity", merchantAcquisitionRepository.countActiveMerchantsByCity());

        return summary;
    }

    // ─── Scheduled Daily KPI Check ──────────────────────────────────

    @Scheduled(cron = "0 0 23 * * *") // Every day at 11 PM
    @Transactional
    public void dailyKpiCheck() {
        log.info("Running daily KPI check");
        Optional<LaunchMetric> todayMetric = launchMetricRepository.findByMetricDate(LocalDate.now());

        if (todayMetric.isEmpty()) {
            log.warn("No metrics recorded for today {}", LocalDate.now());
            return;
        }

        LaunchMetric metric = todayMetric.get();

        // Alert on low success rate
        if (metric.getTxnSuccessRatePct() != null && metric.getTxnSuccessRatePct().doubleValue() < 95.0) {
            log.error("ALERT: Transaction success rate {} is below 95% threshold", metric.getTxnSuccessRatePct());
        }

        // Alert on high avg txn amount anomaly
        if (metric.getAvgTxnAmountPaisa() != null && metric.getAvgTxnAmountPaisa() > 1000000L) {
            log.warn("ALERT: Average transaction amount {}paisa is unusually high", metric.getAvgTxnAmountPaisa());
        }

        // Alert on reconciliation breaks
        if (metric.getReconciliationBreaks() != null && metric.getReconciliationBreaks() > 0) {
            log.warn("ALERT: {} reconciliation breaks reported today", metric.getReconciliationBreaks());
        }

        log.info("Daily KPI check completed - Users: {}, Txns: {}, Success: {}%",
                metric.getTotalRegisteredUsers(), metric.getTotalTransactionsToday(), metric.getTxnSuccessRatePct());
    }
}
