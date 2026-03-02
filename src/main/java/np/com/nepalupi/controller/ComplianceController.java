package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.*;
import np.com.nepalupi.service.compliance.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NRB Compliance & AML API.
 */
@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "NRB Compliance", description = "NRB compliance, AML screening & daily reports")
public class ComplianceController {

    private final ComplianceAuditService auditService;
    private final NrbDailyReportService dailyReportService;
    private final NrbMonthlyReportService monthlyReportService;
    private final NrbQuarterlyReportService quarterlyReportService;
    private final AmlService amlService;

    // ══════════════════════════════════════════════════════════
    //  Audit Log
    // ══════════════════════════════════════════════════════════

    /** Query audit events by type. */
    @GetMapping("/audit/by-type/{eventType}")
    public ResponseEntity<List<ComplianceAuditLog>> auditByType(@PathVariable String eventType) {
        return ResponseEntity.ok(auditService.getByEventType(eventType));
    }

    /** Query audit events by entity. */
    @GetMapping("/audit/by-entity")
    public ResponseEntity<List<ComplianceAuditLog>> auditByEntity(
            @RequestParam String entityType,
            @RequestParam String entityId) {
        return ResponseEntity.ok(auditService.getByEntity(entityType, entityId));
    }

    /** Query audit events by date range. */
    @GetMapping("/audit/by-date")
    public ResponseEntity<List<ComplianceAuditLog>> auditByDate(
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(
                auditService.getByDateRange(LocalDate.parse(start), LocalDate.parse(end)));
    }

    // ══════════════════════════════════════════════════════════
    //  NRB Daily Reports
    // ══════════════════════════════════════════════════════════

    /** Generate a daily report for a specific date. */
    @PostMapping("/reports/daily/generate")
    public ResponseEntity<NrbDailyReport> generateDailyReport(@RequestParam String date) {
        return ResponseEntity.ok(
                dailyReportService.generateForDate(LocalDate.parse(date)));
    }

    /** Get daily report by date. */
    @GetMapping("/reports/daily/{date}")
    public ResponseEntity<NrbDailyReport> getDailyReport(@PathVariable String date) {
        return ResponseEntity.of(dailyReportService.getByDate(LocalDate.parse(date)));
    }

    /** Get daily reports in a date range. */
    @GetMapping("/reports/daily")
    public ResponseEntity<List<NrbDailyReport>> getDailyReports(
            @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity.ok(
                dailyReportService.getByDateRange(LocalDate.parse(start), LocalDate.parse(end)));
    }

    /** Mark a daily report as submitted to NRB. */
    @PostMapping("/reports/daily/{reportId}/submit")
    public ResponseEntity<NrbDailyReport> submitDailyReport(@PathVariable UUID reportId) {
        return ResponseEntity.ok(dailyReportService.markSubmitted(reportId));
    }

    /** Get all un-submitted reports. */
    @GetMapping("/reports/daily/pending")
    public ResponseEntity<List<NrbDailyReport>> getPendingReports() {
        return ResponseEntity.ok(dailyReportService.getPendingSubmission());
    }

    // ══════════════════════════════════════════════════════════
    //  NRB Monthly Reports
    // ══════════════════════════════════════════════════════════

    /** Generate monthly report for a specific month (e.g. 2024-03). */
    @PostMapping("/reports/monthly/generate")
    public ResponseEntity<NrbMonthlyReport> generateMonthlyReport(@RequestParam String month) {
        return ResponseEntity.ok(
                monthlyReportService.generateForMonth(YearMonth.parse(month)));
    }

    /** Get monthly report by month. */
    @GetMapping("/reports/monthly/{month}")
    public ResponseEntity<NrbMonthlyReport> getMonthlyReport(@PathVariable String month) {
        return ResponseEntity.of(monthlyReportService.getByMonth(YearMonth.parse(month)));
    }

    // ══════════════════════════════════════════════════════════
    //  NRB Quarterly Reports
    // ══════════════════════════════════════════════════════════

    /** Generate quarterly risk report (e.g. 2024-Q1). */
    @PostMapping("/reports/quarterly/generate")
    public ResponseEntity<NrbQuarterlyReport> generateQuarterlyReport(@RequestParam String quarter) {
        return ResponseEntity.ok(quarterlyReportService.generateForQuarter(quarter));
    }

    /** Get quarterly report. */
    @GetMapping("/reports/quarterly/{quarter}")
    public ResponseEntity<NrbQuarterlyReport> getQuarterlyReport(@PathVariable String quarter) {
        return ResponseEntity.of(quarterlyReportService.getByQuarter(quarter));
    }

    // ══════════════════════════════════════════════════════════
    //  AML — Suspicious Transaction Reports
    // ══════════════════════════════════════════════════════════

    /** File a suspicious transaction report. */
    @PostMapping("/aml/str")
    public ResponseEntity<SuspiciousTransactionReport> fileStr(
            @RequestParam(required = false) UUID transactionId,
            @RequestParam UUID userId,
            @RequestParam String suspicionType,
            @RequestParam String description) {
        return ResponseEntity.ok(
                amlService.fileStr(transactionId, userId, suspicionType, description, Map.of()));
    }

    /** Review/update STR status. */
    @PostMapping("/aml/str/{strId}/review")
    public ResponseEntity<SuspiciousTransactionReport> reviewStr(
            @PathVariable UUID strId,
            @RequestParam String status,
            @RequestParam String complianceOfficer) {
        return ResponseEntity.ok(amlService.reviewStr(strId, status, complianceOfficer));
    }

    /** Get STRs by status. */
    @GetMapping("/aml/str/by-status/{status}")
    public ResponseEntity<List<SuspiciousTransactionReport>> strByStatus(@PathVariable String status) {
        return ResponseEntity.ok(amlService.getStrsByStatus(status));
    }

    /** Get STRs for a user. */
    @GetMapping("/aml/str/by-user/{userId}")
    public ResponseEntity<List<SuspiciousTransactionReport>> strByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(amlService.getStrsByUser(userId));
    }

    // ══════════════════════════════════════════════════════════
    //  AML — Sanctions Screening
    // ══════════════════════════════════════════════════════════

    /** Screen a user against all sanctions lists. */
    @PostMapping("/aml/sanctions/screen/{userId}")
    public ResponseEntity<Map<String, Object>> screenUser(@PathVariable UUID userId) {
        boolean matchFound = amlService.screenUser(userId);
        var history = amlService.getScreeningHistory(userId);
        return ResponseEntity.ok(Map.of(
                "matchFound", matchFound,
                "screeningResults", history
        ));
    }

    /** Get sanctions screening history for a user. */
    @GetMapping("/aml/sanctions/{userId}")
    public ResponseEntity<List<SanctionsScreening>> sanctionsHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(amlService.getScreeningHistory(userId));
    }
}
