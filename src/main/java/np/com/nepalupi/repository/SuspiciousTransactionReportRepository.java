package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.SuspiciousTransactionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SuspiciousTransactionReportRepository extends JpaRepository<SuspiciousTransactionReport, UUID> {

    List<SuspiciousTransactionReport> findByStatus(String status);

    List<SuspiciousTransactionReport> findByUserId(UUID userId);

    List<SuspiciousTransactionReport> findBySuspicionType(String suspicionType);

    List<SuspiciousTransactionReport> findByFiledWithFiuFalseAndStatus(String status);

    long countByCreatedAtBetween(Instant start, Instant end);

    long countByFiledWithFiuTrueAndCreatedAtBetween(Instant start, Instant end);
}
