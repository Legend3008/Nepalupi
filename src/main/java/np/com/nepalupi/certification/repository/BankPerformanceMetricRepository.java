package np.com.nepalupi.certification.repository;

import np.com.nepalupi.certification.entity.BankPerformanceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankPerformanceMetricRepository extends JpaRepository<BankPerformanceMetric, UUID> {

    Optional<BankPerformanceMetric> findByBankCodeAndMetricDate(String bankCode, LocalDate date);

    List<BankPerformanceMetric> findByBankCodeOrderByMetricDateDesc(String bankCode);

    List<BankPerformanceMetric> findByMetricDateOrderByBankCode(LocalDate date);

    @Query("SELECT m FROM BankPerformanceMetric m WHERE m.metricDate = :date AND m.belowNetworkAverage = true")
    List<BankPerformanceMetric> findBelowAverageForDate(LocalDate date);

    @Query("SELECT AVG(m.avgResponseTimeMs) FROM BankPerformanceMetric m WHERE m.metricDate = :date")
    Long getNetworkAverageResponseTime(LocalDate date);

    @Query("SELECT AVG(m.errorRatePct) FROM BankPerformanceMetric m WHERE m.metricDate = :date")
    java.math.BigDecimal getNetworkAverageErrorRate(LocalDate date);
}
