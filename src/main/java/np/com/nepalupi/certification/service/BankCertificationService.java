package np.com.nepalupi.certification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.certification.entity.BankCertification;
import np.com.nepalupi.certification.entity.CertTestCase;
import np.com.nepalupi.certification.entity.CertTestResult;
import np.com.nepalupi.certification.enums.*;
import np.com.nepalupi.certification.repository.BankCertificationRepository;
import np.com.nepalupi.certification.repository.CertTestCaseRepository;
import np.com.nepalupi.certification.repository.CertTestResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Bank Integration Certification — manages the 6-stage certification journey:
 * 1. Technical Onboarding Agreement
 * 2. Documentation Handover
 * 3. Self-Certification
 * 4. Formal Certification (mandatory 100%, advisory 80%)
 * 5. Parallel Running (2 weeks, 100 txn/day cap)
 * 6. Full Production
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankCertificationService {

    private final BankCertificationRepository certRepo;
    private final CertTestCaseRepository testCaseRepo;
    private final CertTestResultRepository testResultRepo;

    // ========== Bank Onboarding ==========

    @Transactional
    public BankCertification initiateCertification(String bankCode, String bankName,
                                                     String contactName, String contactEmail,
                                                     String contactPhone) {
        BankCertification cert = BankCertification.builder()
                .bankCode(bankCode)
                .bankName(bankName)
                .technicalContactName(contactName)
                .technicalContactEmail(contactEmail)
                .technicalContactPhone(contactPhone)
                .stage(CertificationStage.TECHNICAL_ONBOARDING)
                .status(CertificationStatus.IN_PROGRESS)
                .build();

        cert = certRepo.save(cert);
        log.info("Bank certification initiated: bank={}, code={}", bankName, bankCode);
        return cert;
    }

    @Transactional
    public BankCertification signAgreement(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        cert.setAgreementSigned(true);
        cert.setAgreementSignedAt(Instant.now());
        log.info("Bank agreement signed: bank={}", cert.getBankName());
        return certRepo.save(cert);
    }

    @Transactional
    public BankCertification issueCredentials(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        cert.setSandboxCredentialsIssued(true);
        cert.setSandboxCredentialsIssuedAt(Instant.now());
        log.info("Sandbox credentials issued: bank={}", cert.getBankName());
        return certRepo.save(cert);
    }

    @Transactional
    public BankCertification deliverDocumentation(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        cert.setDocumentationDelivered(true);
        cert.setDocumentationDeliveredAt(Instant.now());
        cert.setStage(CertificationStage.DOCUMENTATION);
        log.info("Documentation delivered: bank={}", cert.getBankName());
        return certRepo.save(cert);
    }

    @Transactional
    public BankCertification advanceToSelfCertification(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        if (!cert.getDocumentationDelivered() || !cert.getSandboxCredentialsIssued()) {
            throw new IllegalStateException("Documentation and sandbox credentials must be delivered first");
        }
        cert.setStage(CertificationStage.SELF_CERTIFICATION);
        cert.setStatus(CertificationStatus.IN_PROGRESS);
        log.info("Bank advanced to self-certification: bank={}", cert.getBankName());
        return certRepo.save(cert);
    }

    @Transactional
    public BankCertification submitSelfCertification(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        cert.setSelfCertSubmitted(true);
        cert.setSelfCertSubmittedAt(Instant.now());

        // Check results
        List<CertTestResult> results = testResultRepo.findByCertificationIdAndExecutionPhaseOrderByExecutedAt(
                certId, ExecutionPhase.SELF_CERTIFICATION);
        long totalTests = testCaseRepo.count();
        long passed = results.stream().filter(r -> r.getResult() == TestResult.PASSED).count();

        cert.setSelfCertPassed(passed == totalTests);
        log.info("Self-certification submitted: bank={}, passed={}/{}", cert.getBankName(), passed, totalTests);
        return certRepo.save(cert);
    }

    // ========== Formal Certification ==========

    @Transactional
    public BankCertification scheduleFormalCertification(UUID certId, Instant scheduledAt) {
        BankCertification cert = getOrThrow(certId);
        if (!cert.getSelfCertPassed()) {
            throw new IllegalStateException("Self-certification must pass before formal certification");
        }
        cert.setStage(CertificationStage.FORMAL_CERTIFICATION);
        cert.setFormalCertScheduledAt(scheduledAt);
        cert.setStatus(CertificationStatus.IN_PROGRESS);
        log.info("Formal certification scheduled: bank={}, date={}", cert.getBankName(), scheduledAt);
        return certRepo.save(cert);
    }

    @Transactional
    public CertTestResult recordTestResult(UUID certId, UUID testCaseId, ExecutionPhase phase,
                                             TestResult result, String requestSent,
                                             String responseReceived, Long responseTimeMs,
                                             String notes, String executedBy) {
        CertTestResult testResult = CertTestResult.builder()
                .certificationId(certId)
                .testCaseId(testCaseId)
                .executionPhase(phase)
                .result(result)
                .requestSent(requestSent)
                .responseReceived(responseReceived)
                .responseTimeMs(responseTimeMs)
                .notes(notes)
                .executedAt(Instant.now())
                .executedBy(executedBy)
                .build();

        return testResultRepo.save(testResult);
    }

    @Transactional
    public BankCertification completeFormalCertification(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        cert.setFormalCertCompletedAt(Instant.now());

        // Calculate mandatory pass rate (must be 100%)
        List<CertTestCase> mandatoryTests = testCaseRepo.findByIsMandatoryTrueOrderByTestCode();
        long mandatoryPassed = 0;
        for (CertTestCase tc : mandatoryTests) {
            long passed = testResultRepo.countByCertificationIdAndExecutionPhaseAndResult(
                    certId, ExecutionPhase.FORMAL_CERTIFICATION, TestResult.PASSED);
            mandatoryPassed += passed > 0 ? 1 : 0;
        }
        BigDecimal mandatoryRate = mandatoryTests.isEmpty() ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(mandatoryPassed * 100).divide(BigDecimal.valueOf(mandatoryTests.size()), 2, RoundingMode.HALF_UP);
        cert.setMandatoryPassRate(mandatoryRate);

        // Calculate advisory pass rate (must be >= 80%)
        List<CertTestCase> advisoryTests = testCaseRepo.findByIsMandatoryFalseOrderByTestCode();
        long advisoryPassed = testResultRepo.countByCertificationIdAndExecutionPhaseAndResult(
                certId, ExecutionPhase.FORMAL_CERTIFICATION, TestResult.PASSED);
        BigDecimal advisoryRate = advisoryTests.isEmpty() ? BigDecimal.valueOf(100)
                : BigDecimal.valueOf(advisoryPassed * 100).divide(BigDecimal.valueOf(advisoryTests.size()), 2, RoundingMode.HALF_UP);
        cert.setAdvisoryPassRate(advisoryRate);

        boolean passed = mandatoryRate.compareTo(BigDecimal.valueOf(100)) == 0
                && advisoryRate.compareTo(BigDecimal.valueOf(80)) >= 0;
        cert.setFormalCertPassed(passed);
        cert.setStatus(passed ? CertificationStatus.PASSED : CertificationStatus.FAILED);

        log.info("Formal certification completed: bank={}, mandatory={}%, advisory={}%, result={}",
                cert.getBankName(), mandatoryRate, advisoryRate, passed ? "PASSED" : "FAILED");
        return certRepo.save(cert);
    }

    // ========== Parallel Running ==========

    @Transactional
    public BankCertification startParallelRunning(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        if (!cert.getFormalCertPassed()) {
            throw new IllegalStateException("Formal certification must pass before parallel running");
        }
        cert.setStage(CertificationStage.PARALLEL_RUNNING);
        cert.setStatus(CertificationStatus.IN_PROGRESS);
        cert.setParallelStartDate(LocalDate.now());
        cert.setParallelEndDate(LocalDate.now().plusWeeks(2));
        cert.setParallelDailyLimit(100);
        log.info("Parallel running started: bank={}, limit=100/day, endDate={}", cert.getBankName(), cert.getParallelEndDate());
        return certRepo.save(cert);
    }

    @Transactional
    public BankCertification recordParallelAnomaly(UUID certId, String notes) {
        BankCertification cert = getOrThrow(certId);
        cert.setParallelAnomaliesFound(cert.getParallelAnomaliesFound() + 1);
        cert.setNotes((cert.getNotes() == null ? "" : cert.getNotes() + "\n") + "ANOMALY: " + notes);
        log.warn("Parallel running anomaly: bank={}, total={}", cert.getBankName(), cert.getParallelAnomaliesFound());
        return certRepo.save(cert);
    }

    @Transactional
    public BankCertification goLive(UUID certId) {
        BankCertification cert = getOrThrow(certId);
        if (cert.getStage() != CertificationStage.PARALLEL_RUNNING) {
            throw new IllegalStateException("Bank must complete parallel running before going live");
        }
        cert.setStage(CertificationStage.FULL_PRODUCTION);
        cert.setStatus(CertificationStatus.PASSED);
        cert.setProductionGoLiveDate(LocalDate.now());
        cert.setRecertificationDueDate(LocalDate.now().plusYears(1));
        log.info("Bank LIVE on production: bank={}, code={}", cert.getBankName(), cert.getBankCode());
        return certRepo.save(cert);
    }

    // ========== Test Case Management ==========

    @Transactional
    public CertTestCase createTestCase(String testCode, TestCaseCategory category,
                                        boolean mandatory, String title, String description,
                                        String expectedBehavior, String isoTemplate,
                                        String expectedResponseCode) {
        CertTestCase tc = CertTestCase.builder()
                .testCode(testCode)
                .category(category)
                .isMandatory(mandatory)
                .title(title)
                .description(description)
                .expectedBehavior(expectedBehavior)
                .isoMessageTemplate(isoTemplate)
                .expectedResponseCode(expectedResponseCode)
                .build();

        return testCaseRepo.save(tc);
    }

    public List<CertTestCase> getMandatoryTestCases() {
        return testCaseRepo.findByIsMandatoryTrueOrderByTestCode();
    }

    public List<CertTestCase> getTestCasesByCategory(TestCaseCategory category) {
        return testCaseRepo.findByCategoryOrderByTestCode(category);
    }

    public List<CertTestResult> getTestResults(UUID certId) {
        return testResultRepo.findByCertificationIdOrderByExecutedAt(certId);
    }

    public BankCertification getCertification(UUID certId) {
        return getOrThrow(certId);
    }

    public List<BankCertification> getCertificationsByStage(CertificationStage stage) {
        return certRepo.findByStageOrderByCreatedAt(stage);
    }

    private BankCertification getOrThrow(UUID id) {
        return certRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank certification not found: " + id));
    }
}
