package np.com.nepalupi.pspcert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.pspcert.entity.PspAppCertification;
import np.com.nepalupi.pspcert.entity.PspAppReviewItem;
import np.com.nepalupi.pspcert.enums.*;
import np.com.nepalupi.pspcert.repository.PspAppCertificationRepository;
import np.com.nepalupi.pspcert.repository.PspAppReviewItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * PSP App Certification — 7-stage certification journey:
 * 1. Design Review (UX standards, PIN pad via SDK)
 * 2. SDK Integration Verification (cert pinning, no hardcoded keys, screenshot prevention)
 * 3. Functional Testing (all user scenarios on test device)
 * 4. Security Testing (traffic interception, credential extraction, overlay attack)
 * 5. Performance Testing (PIN screen <2s, confirmation <1s, history <3s)
 * 6. Compliance Review (T&C, privacy policy, grievance officer, dispute access)
 * 7. Pilot & Launch
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspAppCertificationService {

    private final PspAppCertificationRepository certRepo;
    private final PspAppReviewItemRepository reviewItemRepo;

    @Transactional
    public PspAppCertification initiateCertification(String pspId, String appName,
                                                       AppPlatform platform, String appVersion,
                                                       String sdkVersion) {
        PspAppCertification cert = PspAppCertification.builder()
                .pspId(pspId)
                .appName(appName)
                .appPlatform(platform)
                .appVersion(appVersion)
                .sdkVersion(sdkVersion)
                .stage(AppCertificationStage.DESIGN_REVIEW)
                .status(AppCertificationStatus.NOT_STARTED)
                .build();

        cert = certRepo.save(cert);
        log.info("PSP app certification initiated: psp={}, app={}, platform={}", pspId, appName, platform);

        // Create standard review checklist items for each stage
        createDesignReviewChecklist(cert.getId());
        createSdkIntegrationChecklist(cert.getId());
        createFunctionalTestChecklist(cert.getId());
        createComplianceChecklist(cert.getId());

        return cert;
    }

    // ========== Stage 1: Design Review ==========

    @Transactional
    public PspAppCertification submitDesign(UUID certId) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setDesignSubmitted(true);
        cert.setDesignSubmittedAt(Instant.now());
        cert.setStatus(AppCertificationStatus.IN_PROGRESS);
        return certRepo.save(cert);
    }

    @Transactional
    public PspAppCertification approveDesign(UUID certId, String feedback) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setDesignApproved(true);
        cert.setDesignFeedback(feedback);
        cert.setStage(AppCertificationStage.SDK_INTEGRATION);
        log.info("Design approved for PSP app: psp={}, app={}", cert.getPspId(), cert.getAppName());
        return certRepo.save(cert);
    }

    @Transactional
    public PspAppCertification rejectDesign(UUID certId, String feedback) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setDesignApproved(false);
        cert.setDesignFeedback(feedback);
        cert.setStatus(AppCertificationStatus.FAILED);
        log.warn("Design REJECTED for PSP app: psp={}, feedback={}", cert.getPspId(), feedback);
        return certRepo.save(cert);
    }

    // ========== Stage 2: SDK Integration ==========

    @Transactional
    public PspAppCertification verifySdkIntegration(UUID certId, boolean sdkOk, boolean certPinning,
                                                       boolean pinPadSdk, boolean noHardcodedKeys,
                                                       boolean permissionsOk, boolean screenshotPrevention) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setSdkVerified(sdkOk);
        cert.setCertPinningVerified(certPinning);
        cert.setPinPadSdkVerified(pinPadSdk);
        cert.setNoHardcodedKeys(noHardcodedKeys);
        cert.setPermissionsMinimal(permissionsOk);
        cert.setScreenshotPreventionVerified(screenshotPrevention);

        boolean allPassed = sdkOk && certPinning && pinPadSdk && noHardcodedKeys && permissionsOk && screenshotPrevention;
        if (allPassed) {
            cert.setStage(AppCertificationStage.FUNCTIONAL_TESTING);
            log.info("SDK integration verified: psp={}", cert.getPspId());
        } else {
            cert.setStatus(AppCertificationStatus.FAILED);
            log.warn("SDK integration FAILED: psp={}, sdk={}, certPin={}, pinPad={}, keys={}, perms={}, screenshot={}",
                    cert.getPspId(), sdkOk, certPinning, pinPadSdk, noHardcodedKeys, permissionsOk, screenshotPrevention);
        }
        return certRepo.save(cert);
    }

    // ========== Stage 3: Functional Testing ==========

    @Transactional
    public PspAppCertification completeFunctionalTesting(UUID certId, boolean passed, String notes) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setFunctionalTestPassed(passed);
        cert.setFunctionalTestNotes(notes);
        if (passed) {
            cert.setStage(AppCertificationStage.SECURITY_TESTING);
        } else {
            cert.setStatus(AppCertificationStatus.FAILED);
        }
        return certRepo.save(cert);
    }

    // ========== Stage 4: Security Testing ==========

    @Transactional
    public PspAppCertification completeSecurityTesting(UUID certId, boolean passed,
                                                         int totalFindings, int criticalFindings,
                                                         String reportUrl) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setSecurityTestPassed(passed);
        cert.setSecurityFindingsCount(totalFindings);
        cert.setSecurityCriticalFindings(criticalFindings);
        cert.setSecurityTestReportUrl(reportUrl);

        if (criticalFindings > 0) {
            cert.setStatus(AppCertificationStatus.FAILED);
            log.warn("Security test FAILED — critical findings: psp={}, critical={}", cert.getPspId(), criticalFindings);
        } else if (passed) {
            cert.setStage(AppCertificationStage.PERFORMANCE_TESTING);
        } else {
            cert.setStatus(AppCertificationStatus.CONDITIONALLY_PASSED);
        }
        return certRepo.save(cert);
    }

    // ========== Stage 5: Performance Testing ==========

    @Transactional
    public PspAppCertification completePerformanceTesting(UUID certId, int pinScreenMs,
                                                            int confirmationMs, int historyLoadMs) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setAvgPinScreenTimeMs(pinScreenMs);
        cert.setAvgConfirmationTimeMs(confirmationMs);
        cert.setAvgHistoryLoadTimeMs(historyLoadMs);

        // Benchmarks: PIN < 2000ms, confirmation < 1000ms, history < 3000ms
        boolean passed = pinScreenMs <= 2000 && confirmationMs <= 1000 && historyLoadMs <= 3000;
        cert.setPerformanceTestPassed(passed);

        if (passed) {
            cert.setStage(AppCertificationStage.COMPLIANCE_REVIEW);
        } else {
            cert.setStatus(AppCertificationStatus.FAILED);
            log.warn("Performance test FAILED: psp={}, pin={}ms, confirm={}ms, history={}ms",
                    cert.getPspId(), pinScreenMs, confirmationMs, historyLoadMs);
        }
        return certRepo.save(cert);
    }

    // ========== Stage 6: Compliance Review ==========

    @Transactional
    public PspAppCertification completeComplianceReview(UUID certId, boolean terms, boolean privacy,
                                                          boolean grievance, boolean dispute) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setTermsPresent(terms);
        cert.setPrivacyPolicyPresent(privacy);
        cert.setGrievanceOfficerDisplayed(grievance);
        cert.setDisputeAccessible(dispute);

        boolean passed = terms && privacy && grievance && dispute;
        cert.setCompliancePassed(passed);

        if (passed) {
            cert.setStage(AppCertificationStage.PILOT);
        } else {
            cert.setStatus(AppCertificationStatus.FAILED);
        }
        return certRepo.save(cert);
    }

    // ========== Stage 7: Pilot & Launch ==========

    @Transactional
    public PspAppCertification startPilot(UUID certId, int pilotUserCount) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setPilotStartDate(LocalDate.now());
        cert.setPilotEndDate(LocalDate.now().plusWeeks(2));
        cert.setPilotUserCount(pilotUserCount);
        cert.setStatus(AppCertificationStatus.IN_PROGRESS);
        log.info("Pilot started: psp={}, users={}", cert.getPspId(), pilotUserCount);
        return certRepo.save(cert);
    }

    @Transactional
    public PspAppCertification completePilot(UUID certId, String feedbackSummary) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setPilotFeedbackSummary(feedbackSummary);
        cert.setStage(AppCertificationStage.LAUNCHED);
        return certRepo.save(cert);
    }

    @Transactional
    public PspAppCertification launch(UUID certId, String reviewer) {
        PspAppCertification cert = getOrThrow(certId);
        cert.setStage(AppCertificationStage.LAUNCHED);
        cert.setStatus(AppCertificationStatus.PASSED);
        cert.setLaunchedAt(Instant.now());
        cert.setReviewer(reviewer);
        log.info("PSP app LAUNCHED: psp={}, app={}, platform={}", cert.getPspId(), cert.getAppName(), cert.getAppPlatform());
        return certRepo.save(cert);
    }

    // ========== Review Items ==========

    @Transactional
    public PspAppReviewItem reviewItem(UUID itemId, ReviewResult result, String notes, String reviewedBy) {
        PspAppReviewItem item = reviewItemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Review item not found"));
        item.setResult(result);
        item.setReviewerNotes(notes);
        item.setReviewedBy(reviewedBy);
        item.setReviewedAt(Instant.now());
        return reviewItemRepo.save(item);
    }

    public List<PspAppReviewItem> getReviewItems(UUID certId) {
        return reviewItemRepo.findByCertificationIdOrderByCreatedAt(certId);
    }

    public List<PspAppReviewItem> getReviewItemsByStage(UUID certId, String stage) {
        return reviewItemRepo.findByCertificationIdAndReviewStageOrderByCreatedAt(certId, stage);
    }

    public PspAppCertification getCertification(UUID certId) {
        return getOrThrow(certId);
    }

    public List<PspAppCertification> getCertificationsByStage(AppCertificationStage stage) {
        return certRepo.findByStageOrderByCreatedAt(stage);
    }

    public List<PspAppCertification> getCertificationsForPsp(String pspId) {
        return certRepo.findByPspIdOrderByCreatedAtDesc(pspId);
    }

    // ========== Checklist Creation Helpers ==========

    private void createDesignReviewChecklist(UUID certId) {
        String stage = "DESIGN_REVIEW";
        createItem(certId, stage, "Registration flow screens submitted", true);
        createItem(certId, stage, "Payment initiation flow reviewed", true);
        createItem(certId, stage, "PIN entry screen uses SDK PIN pad", true);
        createItem(certId, stage, "Payee name displayed prominently before PIN entry", true);
        createItem(certId, stage, "Transaction confirmation screen reviewed", true);
        createItem(certId, stage, "Transaction history screen reviewed", true);
        createItem(certId, stage, "Error messages are clear and user-friendly", true);
        createItem(certId, stage, "Fraud awareness messaging for large transactions", false);
    }

    private void createSdkIntegrationChecklist(UUID certId) {
        String stage = "SDK_INTEGRATION";
        createItem(certId, stage, "Current approved SDK version used", true);
        createItem(certId, stage, "Certificate pinning active and configured", true);
        createItem(certId, stage, "SDK PIN pad used (no custom implementation)", true);
        createItem(certId, stage, "No API keys hardcoded in binary", true);
        createItem(certId, stage, "Only necessary device permissions requested", true);
        createItem(certId, stage, "Screenshot/screen recording prevention on sensitive screens", true);
    }

    private void createFunctionalTestChecklist(UUID certId) {
        String stage = "FUNCTIONAL_TESTING";
        createItem(certId, stage, "Registration with SIM binding", true);
        createItem(certId, stage, "Bank account discovery", true);
        createItem(certId, stage, "VPA creation", true);
        createItem(certId, stage, "MPIN setup", true);
        createItem(certId, stage, "P2P payment send and receive", true);
        createItem(certId, stage, "Collect request initiation and response", true);
        createItem(certId, stage, "Transaction history and status display", true);
        createItem(certId, stage, "Dispute raising flow", true);
        createItem(certId, stage, "Mandate creation and management", false);
        createItem(certId, stage, "Account settings and VPA management", true);
    }

    private void createComplianceChecklist(UUID certId) {
        String stage = "COMPLIANCE_REVIEW";
        createItem(certId, stage, "Terms and conditions present and accessible", true);
        createItem(certId, stage, "Privacy policy covers UPI data usage", true);
        createItem(certId, stage, "Grievance officer contact prominently displayed", true);
        createItem(certId, stage, "Dispute raising within 3 taps from history", true);
    }

    private void createItem(UUID certId, String stage, String item, boolean mandatory) {
        PspAppReviewItem reviewItem = PspAppReviewItem.builder()
                .certificationId(certId)
                .reviewStage(stage)
                .checklistItem(item)
                .isMandatory(mandatory)
                .result(ReviewResult.PENDING)
                .build();
        reviewItemRepo.save(reviewItem);
    }

    private PspAppCertification getOrThrow(UUID id) {
        return certRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PSP app certification not found: " + id));
    }
}
