package np.com.nepalupi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.dto.request.PspOnboardingRequest;
import np.com.nepalupi.domain.dto.response.PspOnboardingResponse;
import np.com.nepalupi.domain.entity.PspCertificationResult;
import np.com.nepalupi.domain.entity.PspHealthReport;
import np.com.nepalupi.domain.enums.CertificationTestSuite;
import np.com.nepalupi.service.psp.PspCertificationService;
import np.com.nepalupi.service.psp.PspCredentialService;
import np.com.nepalupi.service.psp.PspHealthReportService;
import np.com.nepalupi.service.psp.PspOnboardingService;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PSP Onboarding & Lifecycle Management API.
 */
@RestController
@RequestMapping("/api/v1/psp-onboarding")
@RequiredArgsConstructor
@Tag(name = "PSP Onboarding", description = "PSP onboarding, certification, credentials & health reports")
public class PspOnboardingController {

    private final PspOnboardingService onboardingService;
    private final PspCertificationService certificationService;
    private final PspCredentialService credentialService;
    private final PspHealthReportService healthReportService;

    // ══════════════════════════════════════════════════════════
    //  Onboarding lifecycle
    // ══════════════════════════════════════════════════════════

    /** Submit a new PSP application. */
    @PostMapping("/apply")
    public ResponseEntity<PspOnboardingResponse> apply(
            @Valid @RequestBody PspOnboardingRequest request) {
        return ResponseEntity.ok(onboardingService.submitApplication(request));
    }

    /** Get onboarding status by internal UUID. */
    @GetMapping("/{pspId}")
    public ResponseEntity<PspOnboardingResponse> getStatus(@PathVariable UUID pspId) {
        return ResponseEntity.ok(onboardingService.getOnboardingStatus(pspId));
    }

    /** Get onboarding status by PSP-ID string (e.g. PSP-ESEWA-12345). */
    @GetMapping("/by-psp-id/{pspId}")
    public ResponseEntity<PspOnboardingResponse> getByPspId(@PathVariable String pspId) {
        return ResponseEntity.ok(onboardingService.getByPspId(pspId));
    }

    /** List all PSPs at a given onboarding stage. */
    @GetMapping("/stage/{stage}")
    public ResponseEntity<List<PspOnboardingResponse>> getByStage(@PathVariable String stage) {
        return ResponseEntity.ok(onboardingService.getByStage(stage));
    }

    /** Advance PSP to the next onboarding stage. */
    @PostMapping("/{pspId}/advance")
    public ResponseEntity<PspOnboardingResponse> advanceStage(
            @PathVariable UUID pspId,
            @RequestParam(defaultValue = "ADMIN") String performedBy,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(onboardingService.advanceStage(pspId, performedBy, notes));
    }

    /** Revert PSP to the previous onboarding stage. */
    @PostMapping("/{pspId}/revert")
    public ResponseEntity<PspOnboardingResponse> revertStage(
            @PathVariable UUID pspId,
            @RequestParam(defaultValue = "ADMIN") String performedBy,
            @RequestParam String reason) {
        return ResponseEntity.ok(onboardingService.revertStage(pspId, performedBy, reason));
    }

    /** Suspend a PSP. */
    @PostMapping("/{pspId}/suspend")
    public ResponseEntity<PspOnboardingResponse> suspend(
            @PathVariable UUID pspId,
            @RequestParam String reason,
            @RequestParam(defaultValue = "ADMIN") String performedBy) {
        return ResponseEntity.ok(onboardingService.suspend(pspId, reason, performedBy));
    }

    /** Reactivate a suspended PSP. */
    @PostMapping("/{pspId}/reactivate")
    public ResponseEntity<PspOnboardingResponse> reactivate(
            @PathVariable UUID pspId,
            @RequestParam(defaultValue = "ADMIN") String performedBy) {
        return ResponseEntity.ok(onboardingService.reactivate(pspId, performedBy));
    }

    // ══════════════════════════════════════════════════════════
    //  Certification
    // ══════════════════════════════════════════════════════════

    /** Record a certification test result. */
    @PostMapping("/{pspId}/certification/{testSuite}")
    public ResponseEntity<PspCertificationResult> recordCertResult(
            @PathVariable UUID pspId,
            @PathVariable CertificationTestSuite testSuite,
            @RequestParam boolean passed,
            @RequestParam(required = false) String details,
            @RequestParam(defaultValue = "TESTER") String testedBy) {
        return ResponseEntity.ok(
                certificationService.recordResult(pspId, testSuite, passed, details, testedBy));
    }

    /** Run all certification tests (simulated). */
    @PostMapping("/{pspId}/certification/run-all")
    public ResponseEntity<List<PspCertificationResult>> runAllTests(
            @PathVariable UUID pspId,
            @RequestParam(defaultValue = "AUTO") String testedBy) {
        return ResponseEntity.ok(certificationService.runAllTests(pspId, testedBy));
    }

    /** Get certification results + summary. */
    @GetMapping("/{pspId}/certification")
    public ResponseEntity<Map<String, Object>> getCertification(@PathVariable UUID pspId) {
        var results = certificationService.getResults(pspId);
        var summary = certificationService.getCertificationSummary(pspId);
        return ResponseEntity.ok(Map.of(
                "results", results,
                "summary", summary
        ));
    }

    // ══════════════════════════════════════════════════════════
    //  Credentials
    // ══════════════════════════════════════════════════════════

    /** Rotate API key + secret. Returns raw credentials (shown once). */
    @PostMapping("/{pspId}/credentials/rotate")
    public ResponseEntity<PspCredentialService.ProductionCredentials> rotateCredentials(
            @PathVariable UUID pspId) {
        return ResponseEntity.ok(credentialService.rotateApiKey(pspId));
    }

    /** Rotate webhook signing secret. */
    @PostMapping("/{pspId}/credentials/rotate-webhook")
    public ResponseEntity<Map<String, String>> rotateWebhookSecret(@PathVariable UUID pspId) {
        String newSecret = credentialService.rotateWebhookSecret(pspId);
        return ResponseEntity.ok(Map.of("webhookSigningSecret", newSecret));
    }

    /** Register client certificate fingerprint (mTLS). */
    @PostMapping("/{pspId}/credentials/client-cert")
    public ResponseEntity<Map<String, String>> registerClientCert(
            @PathVariable UUID pspId,
            @RequestParam String fingerprint) {
        credentialService.registerClientCertificate(pspId, fingerprint);
        return ResponseEntity.ok(Map.of("status", "registered", "fingerprint", fingerprint));
    }

    // ══════════════════════════════════════════════════════════
    //  Health reports
    // ══════════════════════════════════════════════════════════

    /** Get all health reports for a PSP. */
    @GetMapping("/{pspId}/health")
    public ResponseEntity<List<PspHealthReport>> getHealthReports(@PathVariable UUID pspId) {
        return ResponseEntity.ok(healthReportService.getReports(pspId));
    }

    /** Generate a health report on-demand for a specific month. */
    @PostMapping("/{pspId}/health/generate")
    public ResponseEntity<PspHealthReport> generateHealthReport(
            @PathVariable UUID pspId,
            @RequestParam String month) {   // e.g. "2024-03"
        return ResponseEntity.ok(
                healthReportService.generateReport(pspId, YearMonth.parse(month)));
    }

    /** Get health reports across all PSPs for a month. */
    @GetMapping("/health/monthly/{month}")
    public ResponseEntity<List<PspHealthReport>> getMonthlyReports(
            @PathVariable String month) {
        return ResponseEntity.ok(
                healthReportService.getReportsByMonth(YearMonth.parse(month)));
    }
}
