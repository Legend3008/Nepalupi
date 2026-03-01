package np.com.nepalupi.launch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.launch.entity.LaunchChecklistItem;
import np.com.nepalupi.launch.entity.LaunchPhase;
import np.com.nepalupi.launch.enums.*;
import np.com.nepalupi.launch.repository.LaunchChecklistItemRepository;
import np.com.nepalupi.launch.repository.LaunchPhaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LaunchPhaseService {

    private final LaunchPhaseRepository launchPhaseRepository;
    private final LaunchChecklistItemRepository checklistItemRepository;

    // ─── Phase Management ───────────────────────────────────────────

    @Transactional
    public LaunchPhase initializePhases() {
        log.info("Initializing all launch phases for Nepal UPI");

        // Phase 1: Foundation (3 months)
        LaunchPhase foundation = launchPhaseRepository.save(LaunchPhase.builder()
                .phaseName(LaunchPhaseName.FOUNDATION)
                .status(LaunchPhaseStatus.IN_PROGRESS)
                .phaseNumber(1)
                .description("Foundation phase — core infrastructure, regulatory approval, initial bank/PSP onboarding")
                .registrationDailyCap(1000)
                .perTxnLimitPaisa(500000L)
                .dailyLimitPaisa(5000000L)
                .targetBanks(5)
                .targetPspApps(2)
                .targetMerchants(500)
                .targetRegisteredUsers(10000)
                .actualStartDate(LocalDate.now())
                .build());
        createFoundationChecklist(foundation.getId());

        // Phase 2: Controlled Launch (3 months)
        LaunchPhase controlled = launchPhaseRepository.save(LaunchPhase.builder()
                .phaseName(LaunchPhaseName.CONTROLLED_LAUNCH)
                .status(LaunchPhaseStatus.NOT_STARTED)
                .phaseNumber(2)
                .description("Controlled launch — scaled user acquisition, merchant onboarding, feature expansion")
                .registrationDailyCap(50000)
                .perTxnLimitPaisa(1000000L)
                .dailyLimitPaisa(10000000L)
                .targetBanks(15)
                .targetPspApps(5)
                .targetMerchants(5000)
                .targetRegisteredUsers(100000)
                .build());
        createControlledLaunchChecklist(controlled.getId());

        // Phase 3: Accelerated Growth (6 months)
        LaunchPhase accelerated = launchPhaseRepository.save(LaunchPhase.builder()
                .phaseName(LaunchPhaseName.ACCELERATED_GROWTH)
                .status(LaunchPhaseStatus.NOT_STARTED)
                .phaseNumber(3)
                .description("Accelerated growth — government integrations, cross-border pilot, mass adoption")
                .registrationDailyCap(500000)
                .perTxnLimitPaisa(5000000L)
                .dailyLimitPaisa(50000000L)
                .targetBanks(25)
                .targetPspApps(10)
                .targetMerchants(50000)
                .targetRegisteredUsers(1000000)
                .build());
        createAcceleratedGrowthChecklist(accelerated.getId());

        // Phase 4: Ecosystem Expansion (ongoing)
        LaunchPhase ecosystem = launchPhaseRepository.save(LaunchPhase.builder()
                .phaseName(LaunchPhaseName.ECOSYSTEM_EXPANSION)
                .status(LaunchPhaseStatus.NOT_STARTED)
                .phaseNumber(4)
                .description("Ecosystem expansion — credit on UPI, transit, cross-border, full automation")
                .targetBanks(50)
                .targetPspApps(20)
                .targetMerchants(200000)
                .targetRegisteredUsers(5000000)
                .build());
        createEcosystemExpansionChecklist(ecosystem.getId());

        log.info("All 4 launch phases initialized with checklists");
        return foundation;
    }

    public List<LaunchPhase> getAllPhases() {
        return launchPhaseRepository.findAllByOrderByPhaseNumberAsc();
    }

    public LaunchPhase getActivePhase() {
        return launchPhaseRepository.findByStatus(LaunchPhaseStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No active launch phase found"));
    }

    @Transactional
    public LaunchPhase activatePhase(LaunchPhaseName phaseName) {
        LaunchPhase phase = launchPhaseRepository.findByPhaseName(phaseName)
                .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + phaseName));

        // Verify previous phase is completed
        int previousNumber = phase.getPhaseNumber() - 1;
        if (previousNumber > 0) {
            LaunchPhase previousPhase = launchPhaseRepository.findAllByOrderByPhaseNumberAsc().stream()
                    .filter(p -> p.getPhaseNumber() == previousNumber)
                    .findFirst()
                    .orElse(null);
            if (previousPhase != null && previousPhase.getStatus() != LaunchPhaseStatus.COMPLETED) {
                throw new IllegalStateException("Previous phase " + previousPhase.getPhaseName() + " is not completed");
            }
        }

        // Check all blocking checklist items are completed
        long blockingIncomplete = checklistItemRepository.countIncompleteBlockingItems(phase.getId());
        if (blockingIncomplete > 0) {
            throw new IllegalStateException(blockingIncomplete + " blocking checklist items are incomplete for phase " + phaseName);
        }

        phase.setStatus(LaunchPhaseStatus.IN_PROGRESS);
        phase.setActualStartDate(LocalDate.now());
        log.info("Activated launch phase: {}", phaseName);
        return launchPhaseRepository.save(phase);
    }

    @Transactional
    public LaunchPhase completePhase(LaunchPhaseName phaseName) {
        LaunchPhase phase = launchPhaseRepository.findByPhaseName(phaseName)
                .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + phaseName));

        if (phase.getStatus() != LaunchPhaseStatus.IN_PROGRESS) {
            throw new IllegalStateException("Phase " + phaseName + " is not active");
        }

        phase.setStatus(LaunchPhaseStatus.COMPLETED);
        phase.setActualEndDate(LocalDate.now());
        log.info("Completed launch phase: {}", phaseName);
        return launchPhaseRepository.save(phase);
    }

    // ─── Checklist Management ───────────────────────────────────────

    public List<LaunchChecklistItem> getChecklist(UUID phaseId) {
        return checklistItemRepository.findByPhaseIdOrderByCategoryAsc(phaseId);
    }

    public List<LaunchChecklistItem> getChecklistByPhaseName(LaunchPhaseName phaseName) {
        LaunchPhase phase = launchPhaseRepository.findByPhaseName(phaseName)
                .orElseThrow(() -> new IllegalArgumentException("Phase not found: " + phaseName));
        return checklistItemRepository.findByPhaseIdOrderByCategoryAsc(phase.getId());
    }

    public List<LaunchChecklistItem> getIncompleteBlockingItems(UUID phaseId) {
        return checklistItemRepository.findIncompleteBlockingItems(phaseId);
    }

    @Transactional
    public LaunchChecklistItem updateChecklistItem(UUID itemId, ChecklistItemStatus status) {
        LaunchChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Checklist item not found"));
        item.setStatus(status);
        if (status == ChecklistItemStatus.COMPLETED) {
            item.setCompletedAt(Instant.now());
        }
        return checklistItemRepository.save(item);
    }

    // ─── Phase-specific Checklist Creation ──────────────────────────

    private void createFoundationChecklist(UUID phaseId) {
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Core UPI switch deployed and tested", true, "Engineering");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "NCHL/connectIPS integration certified", true, "Engineering");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "HSM infrastructure provisioned", true, "Security");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Disaster recovery tested", true, "Infrastructure");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Load test: 100 TPS sustained", true, "Performance");

        createItem(phaseId, ChecklistCategory.REGULATORY, "NRB license obtained", true, "Compliance");
        createItem(phaseId, ChecklistCategory.REGULATORY, "Data localization compliance verified", true, "Legal");
        createItem(phaseId, ChecklistCategory.REGULATORY, "AML/CFT framework approved by NRB", true, "Compliance");

        createItem(phaseId, ChecklistCategory.BANKING, "VAPT completed by CERT-approved firm", true, "Security");
        createItem(phaseId, ChecklistCategory.BANKING, "PCI-DSS Level 1 certification", true, "Security");
        createItem(phaseId, ChecklistCategory.BANKING, "SOC 2 Type II audit initiated", false, "Security");

        createItem(phaseId, ChecklistCategory.COMMERCIAL, "Minimum 3 banks onboarded", true, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "At least 1 PSP app certified", true, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "Merchant MDR structure approved", true, "Business");

        createItem(phaseId, ChecklistCategory.OPERATIONAL, "24x7 NOC established", true, "Operations");
        createItem(phaseId, ChecklistCategory.OPERATIONAL, "Incident response runbooks created", true, "Operations");
        createItem(phaseId, ChecklistCategory.OPERATIONAL, "Customer grievance portal launched", false, "Operations");
    }

    private void createControlledLaunchChecklist(UUID phaseId) {
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Scaling to 500 TPS validated", true, "Engineering");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Auto-failover tested in production", true, "Infrastructure");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Merchant QR system operational", true, "Engineering");

        createItem(phaseId, ChecklistCategory.REGULATORY, "First monthly NRB report submitted", true, "Compliance");
        createItem(phaseId, ChecklistCategory.REGULATORY, "Consumer protection framework live", true, "Legal");

        createItem(phaseId, ChecklistCategory.BANKING, "Bug bounty program launched", false, "Security");
        createItem(phaseId, ChecklistCategory.BANKING, "WAF rules production-hardened", true, "Security");

        createItem(phaseId, ChecklistCategory.COMMERCIAL, "10+ banks onboarded", true, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "3+ PSP apps in market", true, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "First cashback campaign launched", false, "Marketing");

        createItem(phaseId, ChecklistCategory.OPERATIONAL, "SLA monitoring dashboard live", true, "Operations");
        createItem(phaseId, ChecklistCategory.OPERATIONAL, "Dispute resolution process tested", true, "Operations");
    }

    private void createAcceleratedGrowthChecklist(UUID phaseId) {
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Multi-region deployment active", true, "Infrastructure");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "1000+ TPS capacity validated", true, "Engineering");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "UPI Collect & Mandate features live", false, "Engineering");

        createItem(phaseId, ChecklistCategory.REGULATORY, "Cross-border pilot with India UPI approved", false, "Compliance");

        createItem(phaseId, ChecklistCategory.COMMERCIAL, "Government payment integration (1+ agency)", true, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "50K+ merchants onboarded", true, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "Utility bill payment integration", true, "Business");

        createItem(phaseId, ChecklistCategory.OPERATIONAL, "AI-based fraud detection operational", false, "Security");
        createItem(phaseId, ChecklistCategory.OPERATIONAL, "Automated settlement reconciliation", true, "Operations");
    }

    private void createEcosystemExpansionChecklist(UUID phaseId) {
        createItem(phaseId, ChecklistCategory.TECHNICAL, "5000+ TPS capacity", false, "Engineering");
        createItem(phaseId, ChecklistCategory.TECHNICAL, "Credit on UPI framework", false, "Engineering");

        createItem(phaseId, ChecklistCategory.REGULATORY, "Nepal-India cross-border UPI live", false, "Compliance");

        createItem(phaseId, ChecklistCategory.COMMERCIAL, "Transit/transport integration", false, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "200K+ merchants", false, "Business");
        createItem(phaseId, ChecklistCategory.COMMERCIAL, "IPO/e-commerce deep integration", false, "Business");

        createItem(phaseId, ChecklistCategory.OPERATIONAL, "Full automation of operations", false, "Operations");
    }

    private void createItem(UUID phaseId, ChecklistCategory category,
                            String title, boolean blocking, String owner) {
        checklistItemRepository.save(LaunchChecklistItem.builder()
                .phaseId(phaseId)
                .category(category)
                .title(title)
                .isBlocking(blocking)
                .status(ChecklistItemStatus.PENDING)
                .owner(owner)
                .build());
    }
}
