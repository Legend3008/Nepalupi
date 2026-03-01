package np.com.nepalupi.pspcert.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.pspcert.entity.*;
import np.com.nepalupi.pspcert.enums.*;
import np.com.nepalupi.pspcert.service.PspAppCertificationService;
import np.com.nepalupi.pspcert.service.PspSdkVersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/psp-certification")
@RequiredArgsConstructor
public class PspCertificationController {

    private final PspAppCertificationService certService;
    private final PspSdkVersionService sdkService;

    // ========== Certification Journey ==========

    @PostMapping("/apps")
    public ResponseEntity<PspAppCertification> initiate(
            @RequestParam String pspId, @RequestParam String appName,
            @RequestParam AppPlatform platform, @RequestParam String appVersion,
            @RequestParam String sdkVersion) {
        return ResponseEntity.ok(certService.initiateCertification(pspId, appName, platform, appVersion, sdkVersion));
    }

    @PutMapping("/apps/{id}/submit-design")
    public ResponseEntity<PspAppCertification> submitDesign(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.submitDesign(id));
    }

    @PutMapping("/apps/{id}/approve-design")
    public ResponseEntity<PspAppCertification> approveDesign(@PathVariable UUID id,
                                                               @RequestParam(required = false) String feedback) {
        return ResponseEntity.ok(certService.approveDesign(id, feedback));
    }

    @PutMapping("/apps/{id}/reject-design")
    public ResponseEntity<PspAppCertification> rejectDesign(@PathVariable UUID id,
                                                              @RequestParam String feedback) {
        return ResponseEntity.ok(certService.rejectDesign(id, feedback));
    }

    @PutMapping("/apps/{id}/verify-sdk")
    public ResponseEntity<PspAppCertification> verifySdk(
            @PathVariable UUID id,
            @RequestParam boolean sdkOk, @RequestParam boolean certPinning,
            @RequestParam boolean pinPadSdk, @RequestParam boolean noHardcodedKeys,
            @RequestParam boolean permissionsOk, @RequestParam boolean screenshotPrevention) {
        return ResponseEntity.ok(certService.verifySdkIntegration(id, sdkOk, certPinning,
                pinPadSdk, noHardcodedKeys, permissionsOk, screenshotPrevention));
    }

    @PutMapping("/apps/{id}/functional-test")
    public ResponseEntity<PspAppCertification> functionalTest(@PathVariable UUID id,
                                                                @RequestParam boolean passed,
                                                                @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(certService.completeFunctionalTesting(id, passed, notes));
    }

    @PutMapping("/apps/{id}/security-test")
    public ResponseEntity<PspAppCertification> securityTest(
            @PathVariable UUID id, @RequestParam boolean passed,
            @RequestParam int totalFindings, @RequestParam int criticalFindings,
            @RequestParam(required = false) String reportUrl) {
        return ResponseEntity.ok(certService.completeSecurityTesting(id, passed, totalFindings, criticalFindings, reportUrl));
    }

    @PutMapping("/apps/{id}/performance-test")
    public ResponseEntity<PspAppCertification> performanceTest(
            @PathVariable UUID id,
            @RequestParam int pinScreenMs, @RequestParam int confirmationMs,
            @RequestParam int historyLoadMs) {
        return ResponseEntity.ok(certService.completePerformanceTesting(id, pinScreenMs, confirmationMs, historyLoadMs));
    }

    @PutMapping("/apps/{id}/compliance-review")
    public ResponseEntity<PspAppCertification> complianceReview(
            @PathVariable UUID id,
            @RequestParam boolean terms, @RequestParam boolean privacy,
            @RequestParam boolean grievance, @RequestParam boolean dispute) {
        return ResponseEntity.ok(certService.completeComplianceReview(id, terms, privacy, grievance, dispute));
    }

    @PutMapping("/apps/{id}/start-pilot")
    public ResponseEntity<PspAppCertification> startPilot(@PathVariable UUID id,
                                                            @RequestParam int pilotUserCount) {
        return ResponseEntity.ok(certService.startPilot(id, pilotUserCount));
    }

    @PutMapping("/apps/{id}/complete-pilot")
    public ResponseEntity<PspAppCertification> completePilot(@PathVariable UUID id,
                                                               @RequestParam String feedbackSummary) {
        return ResponseEntity.ok(certService.completePilot(id, feedbackSummary));
    }

    @PutMapping("/apps/{id}/launch")
    public ResponseEntity<PspAppCertification> launch(@PathVariable UUID id,
                                                        @RequestParam String reviewer) {
        return ResponseEntity.ok(certService.launch(id, reviewer));
    }

    @GetMapping("/apps/{id}")
    public ResponseEntity<PspAppCertification> getCertification(@PathVariable UUID id) {
        return ResponseEntity.ok(certService.getCertification(id));
    }

    @GetMapping("/apps/psp/{pspId}")
    public ResponseEntity<List<PspAppCertification>> getForPsp(@PathVariable String pspId) {
        return ResponseEntity.ok(certService.getCertificationsForPsp(pspId));
    }

    @GetMapping("/apps/stage/{stage}")
    public ResponseEntity<List<PspAppCertification>> getByStage(@PathVariable AppCertificationStage stage) {
        return ResponseEntity.ok(certService.getCertificationsByStage(stage));
    }

    // ========== Review Items ==========

    @GetMapping("/apps/{certId}/review-items")
    public ResponseEntity<List<PspAppReviewItem>> getReviewItems(@PathVariable UUID certId) {
        return ResponseEntity.ok(certService.getReviewItems(certId));
    }

    @PutMapping("/review-items/{itemId}")
    public ResponseEntity<PspAppReviewItem> reviewItem(
            @PathVariable UUID itemId, @RequestParam ReviewResult result,
            @RequestParam(required = false) String notes, @RequestParam String reviewedBy) {
        return ResponseEntity.ok(certService.reviewItem(itemId, result, notes, reviewedBy));
    }

    // ========== SDK Version Management ==========

    @PostMapping("/sdk")
    public ResponseEntity<PspSdkVersion> registerSdk(
            @RequestParam String pspId, @RequestParam String platform,
            @RequestParam String currentVersion, @RequestParam String latestVersion) {
        return ResponseEntity.ok(sdkService.registerSdkVersion(pspId, platform, currentVersion, latestVersion));
    }

    @PutMapping("/sdk/{sdkId}/notify")
    public ResponseEntity<PspSdkVersion> notifyUpgrade(@PathVariable UUID sdkId) {
        return ResponseEntity.ok(sdkService.notifyUpgrade(sdkId));
    }

    @PutMapping("/sdk/{sdkId}/confirm-upgrade")
    public ResponseEntity<PspSdkVersion> confirmUpgrade(@PathVariable UUID sdkId,
                                                          @RequestParam String newVersion) {
        return ResponseEntity.ok(sdkService.confirmUpgrade(sdkId, newVersion));
    }

    @GetMapping("/sdk/outdated")
    public ResponseEntity<List<PspSdkVersion>> getOutdatedSdks() {
        return ResponseEntity.ok(sdkService.getOutdatedSdks());
    }

    @GetMapping("/sdk/psp/{pspId}")
    public ResponseEntity<List<PspSdkVersion>> getSdkForPsp(@PathVariable String pspId) {
        return ResponseEntity.ok(sdkService.getSdkVersionsForPsp(pspId));
    }
}
