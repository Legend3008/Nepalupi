package np.com.nepalupi.service.psp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Psp;
import np.com.nepalupi.domain.entity.PspCertificationResult;
import np.com.nepalupi.domain.enums.CertificationTestSuite;
import np.com.nepalupi.repository.PspCertificationResultRepository;
import np.com.nepalupi.repository.PspRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Runs certification test suites against PSPs.
 * <p>
 * Pass criteria: 100% mandatory + ≥80% advisory tests passed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspCertificationService {

    private final PspCertificationResultRepository certResultRepository;
    private final PspRepository pspRepository;

    /**
     * Record the result of a single test suite run.
     */
    @Transactional
    public PspCertificationResult recordResult(UUID pspId, CertificationTestSuite testSuite,
                                                boolean passed, String details, String testedBy) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        String pspIdStr = psp.getPspId();

        // Upsert: if already run for this suite, update
        List<PspCertificationResult> existing = certResultRepository
                .findByPspIdAndTestSuite(pspIdStr, testSuite.name());
        PspCertificationResult result;
        if (!existing.isEmpty()) {
            result = existing.get(0);
        } else {
            result = PspCertificationResult.builder()
                    .pspId(pspIdStr)
                    .testSuite(testSuite.name())
                    .testCase(testSuite.name()) // default test case = suite name
                    .mandatory(testSuite.ordinal() < 6) // first 6 are mandatory
                    .build();
        }

        result.setPassed(passed);
        result.setDetails(details);

        result = certResultRepository.save(result);
        log.info("Certification result recorded: pspId={}, suite={}, passed={}",
                pspIdStr, testSuite.name(), passed);

        return result;
    }

    /**
     * Get all certification results for a PSP.
     */
    public List<PspCertificationResult> getResults(UUID pspId) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));
        return certResultRepository.findByPspIdOrderByExecutedAtDesc(psp.getPspId());
    }

    /**
     * Check whether PSP has cleared certification: 100% mandatory + ≥80% advisory.
     */
    public CertificationSummary getCertificationSummary(UUID pspId) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));
        String pspIdStr = psp.getPspId();

        long mandatoryPassed = certResultRepository.countMandatoryPassed(pspIdStr);
        long mandatoryTotal = certResultRepository.countMandatoryTotal(pspIdStr);
        long advisoryPassed = certResultRepository.countAdvisoryPassed(pspIdStr);
        long advisoryTotal = certResultRepository.countAdvisoryTotal(pspIdStr);

        boolean mandatoryCleared = mandatoryTotal > 0 && mandatoryPassed == mandatoryTotal;
        boolean advisoryCleared = advisoryTotal == 0
                || ((double) advisoryPassed / advisoryTotal) >= 0.8;

        boolean overallCleared = mandatoryCleared && advisoryCleared;

        return new CertificationSummary(
                (int) mandatoryPassed, (int) mandatoryTotal,
                (int) advisoryPassed, (int) advisoryTotal,
                mandatoryCleared, advisoryCleared, overallCleared
        );
    }

    /**
     * Run all test suites in mock/simulation mode — useful for sandbox testing.
     */
    @Transactional
    public List<PspCertificationResult> runAllTests(UUID pspId, String testedBy) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));
        log.info("Running all certification tests for PSP {}", psp.getPspId());

        for (CertificationTestSuite suite : CertificationTestSuite.values()) {
            boolean passed = simulateTest(psp.getPspId(), suite);
            recordResult(pspId, suite, passed, "Auto-run simulation", testedBy);
        }

        return certResultRepository.findByPspIdOrderByExecutedAtDesc(psp.getPspId());
    }

    private boolean simulateTest(String pspId, CertificationTestSuite suite) {
        log.debug("Simulating test suite {} for PSP {}", suite.name(), pspId);
        return true;
    }

    public record CertificationSummary(
            int mandatoryPassed, int mandatoryTotal,
            int advisoryPassed, int advisoryTotal,
            boolean mandatoryCleared, boolean advisoryCleared,
            boolean overallCleared
    ) {}
}
