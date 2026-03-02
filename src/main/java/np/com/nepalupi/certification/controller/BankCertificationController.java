package np.com.nepalupi.certification.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.certification.entity.*;
import np.com.nepalupi.certification.enums.*;
import np.com.nepalupi.certification.service.BankCertificationService;
import np.com.nepalupi.certification.service.BankPerformanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certification")
@RequiredArgsConstructor
@Tag(name = "Bank Certification", description = "Bank technical certification & performance benchmarks")
public class BankCertificationController {

    private final BankCertificationService certService;
    private final BankPerformanceService perfService;

    // ========== Certification Journey ==========

    @PostMapping("/banks")
    public ResponseEntity<BankCertification> initiate(
            @RequestParam String bankCode, @RequestParam String bankName,
            @RequestParam String contactName, @RequestParam String contactEmail,
            @RequestParam String contactPhone) {
        return ResponseEntity.ok(certService.initiateCertification(bankCode, bankName,
                contactName, contactEmail, contactPhone));
    }

    @PutMapping("/banks/{id}/sign-agreement")
    public ResponseEntity<BankCertification> signAgreement(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.signAgreement(id));
    }

    @PutMapping("/banks/{id}/issue-credentials")
    public ResponseEntity<BankCertification> issueCredentials(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.issueCredentials(id));
    }

    @PutMapping("/banks/{id}/deliver-docs")
    public ResponseEntity<BankCertification> deliverDocs(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.deliverDocumentation(id));
    }

    @PutMapping("/banks/{id}/advance-self-cert")
    public ResponseEntity<BankCertification> advanceToSelfCert(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.advanceToSelfCertification(id));
    }

    @PutMapping("/banks/{id}/submit-self-cert")
    public ResponseEntity<BankCertification> submitSelfCert(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.submitSelfCertification(id));
    }

    @PutMapping("/banks/{id}/schedule-formal")
    public ResponseEntity<BankCertification> scheduleFormal(@PathVariable UUID id,
                                                              @RequestParam String scheduledAt) {
        return ResponseEntity.ok(certService.scheduleFormalCertification(id, Instant.parse(scheduledAt)));
    }

    @PostMapping("/banks/{id}/test-results")
    public ResponseEntity<CertTestResult> recordResult(
            @PathVariable UUID id,
            @RequestParam UUID testCaseId, @RequestParam ExecutionPhase phase,
            @RequestParam TestResult result,
            @RequestParam(required = false) String requestSent,
            @RequestParam(required = false) String responseReceived,
            @RequestParam(required = false) Long responseTimeMs,
            @RequestParam(required = false) String notes,
            @RequestParam String executedBy) {
        return ResponseEntity.ok(certService.recordTestResult(id, testCaseId, phase,
                result, requestSent, responseReceived, responseTimeMs, notes, executedBy));
    }

    @PutMapping("/banks/{id}/complete-formal")
    public ResponseEntity<BankCertification> completeFormal(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.completeFormalCertification(id));
    }

    @PutMapping("/banks/{id}/start-parallel")
    public ResponseEntity<BankCertification> startParallel(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.startParallelRunning(id));
    }

    @PostMapping("/banks/{id}/parallel-anomaly")
    public ResponseEntity<BankCertification> recordAnomaly(@PathVariable UUID id,
                                                             @RequestParam String notes) {
        return ResponseEntity.ok(certService.recordParallelAnomaly(id, notes));
    }

    @PutMapping("/banks/{id}/go-live")
    public ResponseEntity<BankCertification> goLive(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.goLive(id));
    }

    @GetMapping("/banks/{id}")
    public ResponseEntity<BankCertification> getCertification(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.getCertification(id));
    }

    @GetMapping("/banks/{id}/results")
    public ResponseEntity<List<CertTestResult>> getResults(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.getTestResults(id));
    }

    @GetMapping("/banks/stage/{stage}")
    public ResponseEntity<List<BankCertification>> getByStage(@PathVariable CertificationStage stage) {
        return ResponseEntity.ok(certService.getCertificationsByStage(stage));
    }

    // ========== Test Cases ==========

    @PostMapping("/test-cases")
    public ResponseEntity<CertTestCase> createTestCase(
            @RequestParam String testCode, @RequestParam TestCaseCategory category,
            @RequestParam(defaultValue = "true") boolean mandatory,
            @RequestParam String title, @RequestParam String description,
            @RequestParam String expectedBehavior,
            @RequestParam(required = false) String isoTemplate,
            @RequestParam(required = false) String expectedResponseCode) {
        return ResponseEntity.ok(certService.createTestCase(testCode, category, mandatory,
                title, description, expectedBehavior, isoTemplate, expectedResponseCode));
    }

    @GetMapping("/test-cases/mandatory")
    public ResponseEntity<List<CertTestCase>> getMandatoryTests() {
        return ResponseEntity.ok(certService.getMandatoryTestCases());
    }

    @GetMapping("/test-cases/category/{category}")
    public ResponseEntity<List<CertTestCase>> getTestsByCategory(@PathVariable TestCaseCategory category) {
        return ResponseEntity.ok(certService.getTestCasesByCategory(category));
    }

    // ========== Performance Monitoring ==========

    @GetMapping("/performance/{bankCode}")
    public ResponseEntity<List<BankPerformanceMetric>> getBankPerformance(@PathVariable String bankCode) {
        return ResponseEntity.ok(perfService.getBankHistory(bankCode));
    }

    @GetMapping("/performance/report/{date}")
    public ResponseEntity<List<BankPerformanceMetric>> getDailyReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(perfService.getDailyReport(date));
    }

    @GetMapping("/performance/below-average/{date}")
    public ResponseEntity<List<BankPerformanceMetric>> getBelowAverage(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(perfService.getBelowAverageBanks(date));
    }
}
