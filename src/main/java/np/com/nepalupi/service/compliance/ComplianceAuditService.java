package np.com.nepalupi.service.compliance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.ComplianceAuditLog;
import np.com.nepalupi.repository.ComplianceAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Append-only compliance audit log — records every significant system event.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceAuditService {

    private final ComplianceAuditLogRepository auditLogRepository;

    /**
     * Record a compliance event (append-only, never updated/deleted).
     */
    @Transactional
    public ComplianceAuditLog recordEvent(String eventType, String entityType,
                                           String entityId, String detailsJson) {
        ComplianceAuditLog entry = ComplianceAuditLog.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .details(detailsJson != null ? detailsJson : "{}")
                .build();

        entry = auditLogRepository.save(entry);
        log.debug("Compliance event recorded: type={}, entity={}/{}", eventType, entityType, entityId);
        return entry;
    }

    /**
     * Query events by type.
     */
    public List<ComplianceAuditLog> getByEventType(String eventType) {
        return auditLogRepository.findByEventType(eventType);
    }

    /**
     * Query events by entity.
     */
    public List<ComplianceAuditLog> getByEntity(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Query events in a date range.
     */
    public List<ComplianceAuditLog> getByDateRange(LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay(ZoneId.of("Asia/Kathmandu")).toInstant();
        Instant endInstant = end.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kathmandu")).toInstant();
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startInstant, endInstant);
    }

    /**
     * Count events of a given type in a date range.
     */
    public long countEvents(String eventType, LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay(ZoneId.of("Asia/Kathmandu")).toInstant();
        Instant endInstant = end.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kathmandu")).toInstant();
        return auditLogRepository.countByEventTypeAndCreatedAtBetween(eventType, startInstant, endInstant);
    }
}
