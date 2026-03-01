package np.com.nepalupi.security.repository;

import np.com.nepalupi.security.entity.SecurityFinding;
import np.com.nepalupi.security.enums.FindingSeverity;
import np.com.nepalupi.security.enums.FindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityFindingRepository extends JpaRepository<SecurityFinding, UUID> {

    List<SecurityFinding> findByAuditIdOrderBySeverity(UUID auditId);

    List<SecurityFinding> findByStatusOrderByRemediationDeadline(FindingStatus status);

    List<SecurityFinding> findBySeverityAndStatusOrderByCreatedAt(FindingSeverity severity, FindingStatus status);

    @Query("SELECT f FROM SecurityFinding f WHERE f.status = 'OPEN' AND f.remediationDeadline <= :now")
    List<SecurityFinding> findOverdueFindings(Instant now);

    @Query("SELECT f.severity, COUNT(f) FROM SecurityFinding f WHERE f.status = 'OPEN' GROUP BY f.severity")
    List<Object[]> countOpenFindingsBySeverity();
}
