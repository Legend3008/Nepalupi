package np.com.nepalupi.security.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.security.entity.*;
import np.com.nepalupi.security.enums.*;
import np.com.nepalupi.security.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityAuditService auditService;
    private final BugBountyService bugBountyService;
    private final CertificateManagementService certService;
    private final SecurityIncidentService incidentService;
    private final WafRuleService wafRuleService;

    // ========== Audit Management ==========

    @PostMapping("/audits")
    public ResponseEntity<SecurityAudit> scheduleAudit(@RequestParam AuditType type,
                                                        @RequestParam String auditorFirm,
                                                        @RequestParam String scope) {
        return ResponseEntity.ok(auditService.scheduleAudit(type, auditorFirm, scope));
    }

    @PutMapping("/audits/{auditId}/start")
    public ResponseEntity<SecurityAudit> startAudit(@PathVariable UUID auditId,
                                                     @RequestParam String auditorName) {
        return ResponseEntity.ok(auditService.startAudit(auditId, auditorName));
    }

    @PutMapping("/audits/{auditId}/complete")
    public ResponseEntity<SecurityAudit> completeAudit(@PathVariable UUID auditId,
                                                        @RequestParam String summary,
                                                        @RequestParam String reportUrl) {
        return ResponseEntity.ok(auditService.completeAudit(auditId, summary, reportUrl));
    }

    @PostMapping("/audits/{auditId}/findings")
    public ResponseEntity<SecurityFinding> addFinding(@PathVariable UUID auditId,
                                                       @RequestParam String title,
                                                       @RequestParam String description,
                                                       @RequestParam FindingSeverity severity,
                                                       @RequestParam FindingCategory category,
                                                       @RequestParam String component,
                                                       @RequestParam(required = false) String reproductionSteps,
                                                       @RequestParam(required = false) String recommendedFix) {
        return ResponseEntity.ok(auditService.addFinding(auditId, title, description, severity,
                category, component, reproductionSteps, recommendedFix));
    }

    @PutMapping("/findings/{findingId}/assign")
    public ResponseEntity<SecurityFinding> assignFinding(@PathVariable UUID findingId,
                                                          @RequestParam String assignee) {
        return ResponseEntity.ok(auditService.assignFinding(findingId, assignee));
    }

    @PutMapping("/findings/{findingId}/fix")
    public ResponseEntity<SecurityFinding> markFixed(@PathVariable UUID findingId) {
        return ResponseEntity.ok(auditService.markFindingFixed(findingId));
    }

    @PutMapping("/findings/{findingId}/verify")
    public ResponseEntity<SecurityFinding> verifyFinding(@PathVariable UUID findingId,
                                                          @RequestParam String verifiedBy) {
        return ResponseEntity.ok(auditService.verifyFinding(findingId, verifiedBy));
    }

    @GetMapping("/findings/open")
    public ResponseEntity<List<SecurityFinding>> getOpenFindings() {
        return ResponseEntity.ok(auditService.getOpenFindings());
    }

    @GetMapping("/findings/overdue")
    public ResponseEntity<List<SecurityFinding>> getOverdueFindings() {
        return ResponseEntity.ok(auditService.getOverdueFindings());
    }

    @PutMapping("/audits/{auditId}/submit-nrb")
    public ResponseEntity<SecurityAudit> submitToNrb(@PathVariable UUID auditId) {
        return ResponseEntity.ok(auditService.submitAuditToNrb(auditId));
    }

    @GetMapping("/audits/completed")
    public ResponseEntity<List<SecurityAudit>> getCompletedAudits() {
        return ResponseEntity.ok(auditService.getCompletedAudits());
    }

    @GetMapping("/findings/summary")
    public ResponseEntity<List<Object[]>> getFindingSummary() {
        return ResponseEntity.ok(auditService.getOpenFindingCountsBySeverity());
    }

    // ========== Bug Bounty ==========

    @PostMapping("/disclosures")
    public ResponseEntity<VulnerabilityDisclosure> submitDisclosure(
            @RequestParam String reporterName, @RequestParam String reporterEmail,
            @RequestParam(required = false) String reporterAlias,
            @RequestParam String title, @RequestParam String description,
            @RequestParam FindingSeverity severity, @RequestParam FindingCategory category,
            @RequestParam String affectedSystem,
            @RequestParam(required = false) String reproductionSteps) {
        return ResponseEntity.ok(bugBountyService.submitDisclosure(reporterName, reporterEmail,
                reporterAlias, title, description, severity, category, affectedSystem, reproductionSteps));
    }

    @PutMapping("/disclosures/{id}/acknowledge")
    public ResponseEntity<VulnerabilityDisclosure> acknowledgeDisclosure(@PathVariable UUID id) {
        return ResponseEntity.ok(bugBountyService.acknowledge(id));
    }

    @PutMapping("/disclosures/{id}/triage")
    public ResponseEntity<VulnerabilityDisclosure> triageDisclosure(@PathVariable UUID id,
                                                                      @RequestParam DisclosureStatus status) {
        return ResponseEntity.ok(bugBountyService.triage(id, status));
    }

    @PutMapping("/disclosures/{id}/fix")
    public ResponseEntity<VulnerabilityDisclosure> fixDisclosure(@PathVariable UUID id,
                                                                   @RequestParam long bountyAmountPaisa) {
        return ResponseEntity.ok(bugBountyService.markFixed(id, bountyAmountPaisa));
    }

    @PutMapping("/disclosures/{id}/pay-bounty")
    public ResponseEntity<VulnerabilityDisclosure> payBounty(@PathVariable UUID id) {
        return ResponseEntity.ok(bugBountyService.payBounty(id));
    }

    @GetMapping("/disclosures/pending")
    public ResponseEntity<List<VulnerabilityDisclosure>> getPendingDisclosures() {
        return ResponseEntity.ok(bugBountyService.getPendingDisclosures());
    }

    // ========== Certificate Management ==========

    @PostMapping("/certificates")
    public ResponseEntity<CertificateInventory> registerCert(
            @RequestParam String certName, @RequestParam CertificateType certType,
            @RequestParam String subjectCn, @RequestParam String issuer,
            @RequestParam String serialNumber, @RequestParam String fingerprint,
            @RequestParam String validFrom, @RequestParam String validUntil,
            @RequestParam String service, @RequestParam String keyStoreLocation,
            @RequestParam(defaultValue = "false") boolean autoRotate,
            @RequestParam(required = false) Integer rotationPeriodDays) {
        return ResponseEntity.ok(certService.registerCertificate(certName, certType, subjectCn, issuer,
                serialNumber, fingerprint, java.time.Instant.parse(validFrom), java.time.Instant.parse(validUntil),
                service, keyStoreLocation, autoRotate, rotationPeriodDays));
    }

    @GetMapping("/certificates/active")
    public ResponseEntity<List<CertificateInventory>> getActiveCerts() {
        return ResponseEntity.ok(certService.getActiveCertificates());
    }

    @PutMapping("/certificates/{certId}/revoke")
    public ResponseEntity<Map<String, String>> revokeCert(@PathVariable UUID certId) {
        certService.revokeCertificate(certId);
        return ResponseEntity.ok(Map.of("status", "revoked"));
    }

    // ========== Security Incidents ==========

    @PostMapping("/incidents")
    public ResponseEntity<SecurityIncident> reportIncident(
            @RequestParam SecurityIncidentType type, @RequestParam String severity,
            @RequestParam String title, @RequestParam(required = false) String description,
            @RequestParam(required = false) String attackVector,
            @RequestParam(required = false) String affectedSystems) {
        return ResponseEntity.ok(incidentService.reportIncident(type, severity,
                title, description, attackVector, affectedSystems));
    }

    @PutMapping("/incidents/{id}/contain")
    public ResponseEntity<SecurityIncident> containIncident(@PathVariable UUID id,
                                                              @RequestParam String commander) {
        return ResponseEntity.ok(incidentService.containIncident(id, commander));
    }

    @PutMapping("/incidents/{id}/eradicate")
    public ResponseEntity<SecurityIncident> eradicate(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.eradicate(id));
    }

    @PutMapping("/incidents/{id}/recover")
    public ResponseEntity<SecurityIncident> recover(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.recover(id));
    }

    @PutMapping("/incidents/{id}/close")
    public ResponseEntity<SecurityIncident> closeIncident(@PathVariable UUID id,
                                                            @RequestParam String rootCause,
                                                            @RequestParam String lessonsLearned) {
        return ResponseEntity.ok(incidentService.closeIncident(id, rootCause, lessonsLearned));
    }

    @GetMapping("/incidents/active")
    public ResponseEntity<List<SecurityIncident>> getActiveIncidents() {
        return ResponseEntity.ok(incidentService.getActiveIncidents());
    }

    // ========== WAF Rules ==========

    @PostMapping("/waf/rules")
    public ResponseEntity<WafRule> createWafRule(@RequestParam String ruleName,
                                                  @RequestParam WafRuleType ruleType,
                                                  @RequestParam(defaultValue = "BLOCK") WafAction action,
                                                  @RequestParam(required = false) String pattern,
                                                  @RequestParam(required = false) String description,
                                                  @RequestParam(defaultValue = "100") int priority) {
        return ResponseEntity.ok(wafRuleService.createRule(ruleName, ruleType, action, pattern, description, priority));
    }

    @PutMapping("/waf/rules/{ruleId}/toggle")
    public ResponseEntity<WafRule> toggleWafRule(@PathVariable UUID ruleId,
                                                   @RequestParam boolean active) {
        return ResponseEntity.ok(wafRuleService.toggleRule(ruleId, active));
    }

    @GetMapping("/waf/rules/active")
    public ResponseEntity<List<WafRule>> getActiveWafRules() {
        return ResponseEntity.ok(wafRuleService.getActiveRules());
    }
}
