package np.com.nepalupi.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.security.entity.SecurityAudit;
import np.com.nepalupi.security.entity.SecurityFinding;
import np.com.nepalupi.security.enums.*;
import np.com.nepalupi.security.repository.SecurityAuditRepository;
import np.com.nepalupi.security.repository.SecurityFindingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Manages security audits — internal, external pen tests, red team exercises.
 * Tracks findings through their full lifecycle: open → fixed → verified.
 * Enforces remediation SLAs: critical 7d, high 30d, medium 90d.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditService {

    private final SecurityAuditRepository auditRepository;
    private final SecurityFindingRepository findingRepository;

    @Transactional
    public SecurityAudit scheduleAudit(AuditType type, String auditorFirm, String scope) {
        SecurityAudit audit = SecurityAudit.builder()
                .auditType(type)
                .auditStatus(AuditStatus.SCHEDULED)
                .auditorFirm(auditorFirm)
                .scopeDescription(scope)
                .build();

        audit = auditRepository.save(audit);
        log.info("Security audit scheduled: type={}, firm={}, id={}", type, auditorFirm, audit.getId());
        return audit;
    }

    @Transactional
    public SecurityAudit startAudit(UUID auditId, String auditorName) {
        SecurityAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));

        audit.setAuditStatus(AuditStatus.IN_PROGRESS);
        audit.setStartedAt(Instant.now());
        audit.setAuditorName(auditorName);
        audit = auditRepository.save(audit);

        log.info("Security audit started: id={}, auditor={}", auditId, auditorName);
        return audit;
    }

    @Transactional
    public SecurityAudit completeAudit(UUID auditId, String executiveSummary, String reportUrl) {
        SecurityAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found: " + auditId));

        audit.setAuditStatus(AuditStatus.COMPLETED);
        audit.setCompletedAt(Instant.now());
        audit.setExecutiveSummary(executiveSummary);
        audit.setReportUrl(reportUrl);

        // Calculate next audit due: 12 months for external, 3 months for internal
        long monthsUntilNext = (audit.getAuditType() == AuditType.EXTERNAL_PENTEST
                || audit.getAuditType() == AuditType.RED_TEAM) ? 12 : 3;
        audit.setNextAuditDue(Instant.now().plus(monthsUntilNext * 30, ChronoUnit.DAYS));

        // Count findings by severity
        List<SecurityFinding> findings = findingRepository.findByAuditIdOrderBySeverity(auditId);
        audit.setTotalFindings(findings.size());
        audit.setCriticalFindings((int) findings.stream().filter(f -> f.getSeverity() == FindingSeverity.CRITICAL).count());
        audit.setHighFindings((int) findings.stream().filter(f -> f.getSeverity() == FindingSeverity.HIGH).count());
        audit.setMediumFindings((int) findings.stream().filter(f -> f.getSeverity() == FindingSeverity.MEDIUM).count());
        audit.setLowFindings((int) findings.stream().filter(f -> f.getSeverity() == FindingSeverity.LOW).count());

        audit = auditRepository.save(audit);
        log.info("Security audit completed: id={}, total={}, critical={}, high={}",
                auditId, audit.getTotalFindings(), audit.getCriticalFindings(), audit.getHighFindings());
        return audit;
    }

    @Transactional
    public SecurityFinding addFinding(UUID auditId, String title, String description,
                                       FindingSeverity severity, FindingCategory category,
                                       String affectedComponent, String reproductionSteps,
                                       String recommendedFix) {
        // Generate finding reference
        long count = findingRepository.count();
        String reference = String.format("VULN-%d-%03d", java.time.Year.now().getValue(), count + 1);

        SecurityFinding finding = SecurityFinding.builder()
                .auditId(auditId)
                .findingReference(reference)
                .title(title)
                .description(description)
                .severity(severity)
                .category(category)
                .affectedComponent(affectedComponent)
                .reproductionSteps(reproductionSteps)
                .recommendedFix(recommendedFix)
                .status(FindingStatus.OPEN)
                .build();

        // Set remediation deadline based on severity: CRITICAL=7d, HIGH=30d, MEDIUM=90d, LOW=180d
        Instant deadline = switch (severity) {
            case CRITICAL -> Instant.now().plus(7, ChronoUnit.DAYS);
            case HIGH -> Instant.now().plus(30, ChronoUnit.DAYS);
            case MEDIUM -> Instant.now().plus(90, ChronoUnit.DAYS);
            case LOW -> Instant.now().plus(180, ChronoUnit.DAYS);
            case INFO -> null;
        };
        finding.setRemediationDeadline(deadline);

        finding = findingRepository.save(finding);
        log.info("Security finding added: ref={}, severity={}, component={}",
                reference, severity, affectedComponent);
        return finding;
    }

    @Transactional
    public SecurityFinding assignFinding(UUID findingId, String assignee) {
        SecurityFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found"));
        finding.setAssignedTo(assignee);
        finding.setStatus(FindingStatus.IN_PROGRESS);
        return findingRepository.save(finding);
    }

    @Transactional
    public SecurityFinding markFindingFixed(UUID findingId) {
        SecurityFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found"));
        finding.setStatus(FindingStatus.FIXED);
        finding.setFixedAt(Instant.now());
        return findingRepository.save(finding);
    }

    @Transactional
    public SecurityFinding verifyFinding(UUID findingId, String verifiedBy) {
        SecurityFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new IllegalArgumentException("Finding not found"));
        finding.setStatus(FindingStatus.VERIFIED);
        finding.setVerifiedAt(Instant.now());
        finding.setVerifiedBy(verifiedBy);
        return findingRepository.save(finding);
    }

    @Transactional
    public SecurityAudit submitAuditToNrb(UUID auditId) {
        SecurityAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found"));
        audit.setNrbSubmitted(true);
        audit.setNrbSubmittedAt(Instant.now());
        log.info("Audit report submitted to NRB: id={}", auditId);
        return auditRepository.save(audit);
    }

    public List<SecurityFinding> getOpenFindings() {
        return findingRepository.findByStatusOrderByRemediationDeadline(FindingStatus.OPEN);
    }

    public List<SecurityFinding> getOverdueFindings() {
        return findingRepository.findOverdueFindings(Instant.now());
    }

    /**
     * Daily check for overdue findings — alerts on critical/high findings past their deadline.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void checkOverdueFindings() {
        List<SecurityFinding> overdue = findingRepository.findOverdueFindings(Instant.now());
        if (!overdue.isEmpty()) {
            log.warn("SECURITY ALERT: {} findings are past their remediation deadline", overdue.size());
            overdue.forEach(f -> log.warn("  Overdue: ref={}, severity={}, deadline={}, component={}",
                    f.getFindingReference(), f.getSeverity(), f.getRemediationDeadline(), f.getAffectedComponent()));
        }
    }

    public List<SecurityAudit> getCompletedAudits() {
        return auditRepository.findByAuditStatusOrderByCreatedAtDesc(AuditStatus.COMPLETED);
    }

    public List<Object[]> getOpenFindingCountsBySeverity() {
        return findingRepository.countOpenFindingsBySeverity();
    }
}
