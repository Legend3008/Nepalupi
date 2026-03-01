package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.ComplianceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ComplianceAuditLogRepository extends JpaRepository<ComplianceAuditLog, Long> {

    List<ComplianceAuditLog> findByEventType(String eventType);

    List<ComplianceAuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);

    List<ComplianceAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant start, Instant end);

    List<ComplianceAuditLog> findByEventTypeAndCreatedAtBetween(String eventType, Instant start, Instant end);

    long countByEventTypeAndCreatedAtBetween(String eventType, Instant start, Instant end);
}
