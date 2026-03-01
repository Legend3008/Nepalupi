package np.com.nepalupi.service.psp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.request.PspOnboardingRequest;
import np.com.nepalupi.domain.dto.response.PspOnboardingResponse;
import np.com.nepalupi.domain.entity.Psp;
import np.com.nepalupi.domain.entity.PspOnboardingLog;
import np.com.nepalupi.domain.enums.PspOnboardingStage;
import np.com.nepalupi.domain.enums.PspTier;
import np.com.nepalupi.repository.PspCertificationResultRepository;
import np.com.nepalupi.repository.PspOnboardingLogRepository;
import np.com.nepalupi.repository.PspRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Manages the 6-stage PSP onboarding lifecycle:
 * APPLICATION → LEGAL_AGREEMENT → TECHNICAL_CERTIFICATION → SECURITY_REVIEW → PILOT → PRODUCTION
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspOnboardingService {

    private final PspRepository pspRepository;
    private final PspOnboardingLogRepository onboardingLogRepository;
    private final PspCertificationResultRepository certResultRepository;
    private final PspCredentialService credentialService;

    // ── Application ──────────────────────────────────────────

    @Transactional
    public PspOnboardingResponse submitApplication(PspOnboardingRequest request) {
        String pspId = generatePspId(request.getName());

        Psp psp = Psp.builder()
                .pspId(pspId)
                .name(request.getName())
                .nrbLicenseNumber(request.getNrbLicenseNumber())
                .technicalContactEmail(request.getTechnicalContactEmail())
                .technicalContactPhone(request.getTechnicalContactPhone())
                .webhookUrl(request.getWebhookUrl())
                .onboardingStage(PspOnboardingStage.APPLICATION.name())
                .tier(request.getRequestedTier() != null ? request.getRequestedTier() : 1)
                .apiKeyHash("PENDING")
                .secretHash("PENDING")
                .isActive(false)
                .build();

        // Apply tier limits
        applyTierLimits(psp);

        psp = pspRepository.save(psp);
        logTransition(psp, null, PspOnboardingStage.APPLICATION.name(), "SYSTEM", "Application submitted");

        log.info("PSP application submitted: pspId={}, name={}", pspId, request.getName());
        return buildResponse(psp);
    }

    // ── Stage transitions ────────────────────────────────────

    @Transactional
    public PspOnboardingResponse advanceStage(UUID pspId, String performedBy, String notes) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        PspOnboardingStage current = PspOnboardingStage.valueOf(psp.getOnboardingStage());
        PspOnboardingStage next = getNextStage(current);

        if (next == null) {
            throw new IllegalStateException("PSP is already in PRODUCTION stage");
        }

        // Validate pre-conditions for stage advancement
        validateStageTransition(psp, current, next);

        String fromStage = current.name();
        psp.setOnboardingStage(next.name());

        // Apply stage-specific actions
        applyStageActions(psp, next);

        psp = pspRepository.save(psp);
        logTransition(psp, fromStage, next.name(), performedBy, notes);

        log.info("PSP {} advanced from {} to {}", psp.getPspId(), fromStage, next.name());
        return buildResponse(psp);
    }

    @Transactional
    public PspOnboardingResponse revertStage(UUID pspId, String performedBy, String reason) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        PspOnboardingStage current = PspOnboardingStage.valueOf(psp.getOnboardingStage());
        PspOnboardingStage previous = getPreviousStage(current);

        if (previous == null) {
            throw new IllegalStateException("Cannot revert from APPLICATION stage");
        }

        String fromStage = current.name();
        psp.setOnboardingStage(previous.name());
        psp = pspRepository.save(psp);
        logTransition(psp, fromStage, previous.name(), performedBy, "REVERT: " + reason);

        log.warn("PSP {} reverted from {} to {}: {}", psp.getPspId(), fromStage, previous.name(), reason);
        return buildResponse(psp);
    }

    // ── Suspend / Reactivate ─────────────────────────────────

    @Transactional
    public PspOnboardingResponse suspend(UUID pspId, String reason, String performedBy) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        psp.setIsActive(false);
        psp.setSuspensionReason(reason);
        psp.setSuspendedAt(java.time.Instant.now());
        psp = pspRepository.save(psp);

        logTransition(psp, psp.getOnboardingStage(), "SUSPENDED",
                performedBy, "Suspended: " + reason);

        log.warn("PSP {} suspended: {}", psp.getPspId(), reason);
        return buildResponse(psp);
    }

    @Transactional
    public PspOnboardingResponse reactivate(UUID pspId, String performedBy) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));

        psp.setIsActive(true);
        psp.setSuspensionReason(null);
        psp.setSuspendedAt(null);
        psp = pspRepository.save(psp);

        logTransition(psp, "SUSPENDED", psp.getOnboardingStage(),
                performedBy, "Reactivated");

        log.info("PSP {} reactivated", psp.getPspId());
        return buildResponse(psp);
    }

    // ── Query ────────────────────────────────────────────────

    public PspOnboardingResponse getOnboardingStatus(UUID pspId) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));
        return buildResponse(psp);
    }

    public PspOnboardingResponse getByPspId(String pspId) {
        Psp psp = pspRepository.findByPspId(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));
        return buildResponse(psp);
    }

    public List<PspOnboardingResponse> getByStage(String stage) {
        return pspRepository.findAll().stream()
                .filter(p -> stage.equals(p.getOnboardingStage()))
                .map(this::buildResponse)
                .toList();
    }

    // ── Internal helpers ─────────────────────────────────────

    private void validateStageTransition(Psp psp, PspOnboardingStage current, PspOnboardingStage next) {
        switch (next) {
            case LEGAL_AGREEMENT -> {
                if (psp.getNrbLicenseNumber() == null || psp.getNrbLicenseNumber().isBlank()) {
                    throw new IllegalStateException("NRB license number required before LEGAL_AGREEMENT");
                }
            }
            case TECHNICAL_CERTIFICATION -> {
                // Legal agreement must be signed — trust that stage was reached
            }
            case SECURITY_REVIEW -> {
                // All mandatory certification tests must pass
                long mandatoryPassed = certResultRepository.countMandatoryPassed(psp.getPspId());
                long mandatoryTotal = certResultRepository.countMandatoryTotal(psp.getPspId());
                if (mandatoryTotal == 0 || mandatoryPassed < mandatoryTotal) {
                    throw new IllegalStateException(
                            "All mandatory certification tests must pass before SECURITY_REVIEW. " +
                            "Passed: " + mandatoryPassed + "/" + mandatoryTotal);
                }
            }
            case PILOT -> {
                // Security review passed — trust stage transition
            }
            case PRODUCTION -> {
                if (psp.getPilotStartDate() == null) {
                    throw new IllegalStateException("Pilot must have a start date");
                }
                if (psp.getPilotStartDate().plusDays(30).isAfter(LocalDate.now())) {
                    throw new IllegalStateException("Pilot must run for at least 30 days before PRODUCTION");
                }
            }
            default -> { }
        }
    }

    private void applyStageActions(Psp psp, PspOnboardingStage stage) {
        switch (stage) {
            case PILOT -> {
                psp.setPilotStartDate(LocalDate.now());
                // Generate sandbox credentials
                String sandboxToken = credentialService.generateSandboxToken(psp.getId());
                psp.setSandboxToken(sandboxToken);
            }
            case PRODUCTION -> {
                psp.setProductionDate(LocalDate.now());
                psp.setIsActive(true);
                // Generate production credentials
                credentialService.generateProductionCredentials(psp);
            }
            default -> { }
        }
    }

    private void applyTierLimits(Psp psp) {
        PspTier tier = PspTier.fromLevel(psp.getTier());
        psp.setPerTxnLimitPaisa(tier.getPerTxnLimitPaisa());
        psp.setDailyLimitPaisa(tier.getDailyLimitPaisa());
    }

    private PspOnboardingStage getNextStage(PspOnboardingStage current) {
        return switch (current) {
            case APPLICATION -> PspOnboardingStage.LEGAL_AGREEMENT;
            case LEGAL_AGREEMENT -> PspOnboardingStage.TECHNICAL_CERTIFICATION;
            case TECHNICAL_CERTIFICATION -> PspOnboardingStage.SECURITY_REVIEW;
            case SECURITY_REVIEW -> PspOnboardingStage.PILOT;
            case PILOT -> PspOnboardingStage.PRODUCTION;
            case PRODUCTION -> null;
        };
    }

    private PspOnboardingStage getPreviousStage(PspOnboardingStage current) {
        return switch (current) {
            case APPLICATION -> null;
            case LEGAL_AGREEMENT -> PspOnboardingStage.APPLICATION;
            case TECHNICAL_CERTIFICATION -> PspOnboardingStage.LEGAL_AGREEMENT;
            case SECURITY_REVIEW -> PspOnboardingStage.TECHNICAL_CERTIFICATION;
            case PILOT -> PspOnboardingStage.SECURITY_REVIEW;
            case PRODUCTION -> PspOnboardingStage.PILOT;
        };
    }

    private String generatePspId(String name) {
        String prefix = name.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        return "PSP-" + prefix + "-" + System.currentTimeMillis() % 100000;
    }

    private void logTransition(Psp psp, String fromStage, String toStage,
                                String performedBy, String notes) {
        PspOnboardingLog logEntry = PspOnboardingLog.builder()
                .pspId(psp.getPspId())
                .fromStage(fromStage)
                .toStage(toStage)
                .performedBy(performedBy)
                .notes(notes)
                .build();
        onboardingLogRepository.save(logEntry);
    }

    private PspOnboardingResponse buildResponse(Psp psp) {
        List<PspOnboardingLog> logs = onboardingLogRepository.findByPspIdOrderByCreatedAtAsc(psp.getPspId());

        List<PspOnboardingResponse.StageTransition> history = logs.stream()
                .map(l -> PspOnboardingResponse.StageTransition.builder()
                        .fromStage(l.getFromStage())
                        .toStage(l.getToStage())
                        .performedBy(l.getPerformedBy())
                        .notes(l.getNotes())
                        .timestamp(l.getCreatedAt())
                        .build())
                .toList();

        int mandPassed = (int) certResultRepository.countMandatoryPassed(psp.getPspId());
        int mandTotal = (int) certResultRepository.countMandatoryTotal(psp.getPspId());
        int advPassed = (int) certResultRepository.countAdvisoryPassed(psp.getPspId());
        int advTotal = (int) certResultRepository.countAdvisoryTotal(psp.getPspId());

        return PspOnboardingResponse.builder()
                .id(psp.getId())
                .pspId(psp.getPspId())
                .name(psp.getName())
                .onboardingStage(psp.getOnboardingStage())
                .tier(psp.getTier())
                .nrbLicenseNumber(psp.getNrbLicenseNumber())
                .nrbLicenseExpiry(psp.getNrbLicenseExpiry())
                .perTxnLimitPaisa(psp.getPerTxnLimitPaisa())
                .dailyLimitPaisa(psp.getDailyLimitPaisa())
                .pilotStartDate(psp.getPilotStartDate())
                .productionDate(psp.getProductionDate())
                .technicalContactEmail(psp.getTechnicalContactEmail())
                .technicalContactPhone(psp.getTechnicalContactPhone())
                .webhookUrl(psp.getWebhookUrl())
                .isActive(psp.getIsActive())
                .suspensionReason(psp.getSuspensionReason())
                .suspendedAt(psp.getSuspendedAt())
                .createdAt(psp.getCreatedAt())
                .updatedAt(psp.getUpdatedAt())
                .mandatoryTestsPassed(mandPassed)
                .mandatoryTestsTotal(mandTotal)
                .advisoryTestsPassed(advPassed)
                .advisoryTestsTotal(advTotal)
                .stageHistory(history)
                .build();
    }
}
