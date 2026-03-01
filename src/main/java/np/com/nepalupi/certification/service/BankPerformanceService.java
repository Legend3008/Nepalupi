package np.com.nepalupi.certification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.certification.entity.BankPerformanceMetric;
import np.com.nepalupi.certification.repository.BankPerformanceMetricRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Monitors bank performance post-certification: response times, error rates,
 * settlement accuracy, timeout rates. Generates monthly reports comparing
 * each bank against network averages. Issues formal performance notices
 * for banks significantly below average.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankPerformanceService {

    private final BankPerformanceMetricRepository metricRepo;

    @Transactional
    public BankPerformanceMetric recordDailyMetrics(String bankCode, LocalDate date,
                                                      long totalTxn, long successTxn, long failedTxn,
                                                      long timeouts, long avgResponseMs,
                                                      long p95Ms, long p99Ms,
                                                      BigDecimal settlementAccuracy, BigDecimal errorRate) {
        BankPerformanceMetric metric = metricRepo.findByBankCodeAndMetricDate(bankCode, date)
                .orElse(BankPerformanceMetric.builder()
                        .bankCode(bankCode)
                        .metricDate(date)
                        .build());

        metric.setTotalTransactions(totalTxn);
        metric.setSuccessfulTransactions(successTxn);
        metric.setFailedTransactions(failedTxn);
        metric.setTimeoutCount(timeouts);
        metric.setAvgResponseTimeMs(avgResponseMs);
        metric.setP95ResponseTimeMs(p95Ms);
        metric.setP99ResponseTimeMs(p99Ms);
        metric.setSettlementAccuracyPct(settlementAccuracy);
        metric.setErrorRatePct(errorRate);

        return metricRepo.save(metric);
    }

    /**
     * Daily at 3 AM: compare each bank's metrics against network average.
     * Flag banks below average and send performance notices.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void evaluateDailyPerformance() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Long networkAvgResponse = metricRepo.getNetworkAverageResponseTime(yesterday);
        BigDecimal networkAvgError = metricRepo.getNetworkAverageErrorRate(yesterday);

        if (networkAvgResponse == null || networkAvgError == null) {
            log.debug("No metrics available for yesterday's performance evaluation");
            return;
        }

        List<BankPerformanceMetric> metrics = metricRepo.findByMetricDateOrderByBankCode(yesterday);
        for (BankPerformanceMetric m : metrics) {
            // Below average if response time > 1.5x network avg OR error rate > 2x network avg
            boolean belowAvg = m.getAvgResponseTimeMs() > (networkAvgResponse * 1.5)
                    || m.getErrorRatePct().compareTo(networkAvgError.multiply(BigDecimal.valueOf(2))) > 0;

            m.setBelowNetworkAverage(belowAvg);
            if (belowAvg && !m.getPerformanceNoticeSent()) {
                m.setPerformanceNoticeSent(true);
                m.setPerformanceNoticeSentAt(Instant.now());
                log.warn("PERFORMANCE NOTICE: bank={}, avgResponse={}ms (network={}ms), errorRate={}% (network={}%)",
                        m.getBankCode(), m.getAvgResponseTimeMs(), networkAvgResponse,
                        m.getErrorRatePct(), networkAvgError);
                // In production: send formal notice to bank's technical contact
            }
            metricRepo.save(m);
        }
    }

    public List<BankPerformanceMetric> getBankHistory(String bankCode) {
        return metricRepo.findByBankCodeOrderByMetricDateDesc(bankCode);
    }

    public List<BankPerformanceMetric> getDailyReport(LocalDate date) {
        return metricRepo.findByMetricDateOrderByBankCode(date);
    }

    public List<BankPerformanceMetric> getBelowAverageBanks(LocalDate date) {
        return metricRepo.findBelowAverageForDate(date);
    }
}
