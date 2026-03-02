package np.com.nepalupi.service.psp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Psp;
import np.com.nepalupi.domain.entity.Tpap;
import np.com.nepalupi.repository.PspRepository;
import np.com.nepalupi.repository.TpapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * TPAP Service — manages Third Party Application Providers.
 * <p>
 * In the UPI architecture, a TPAP (like Google Pay, CRED) uses a sponsor PSP's
 * banking license to offer UPI services. The TPAP doesn't directly connect to
 * NCHL — all routing goes through the sponsor PSP.
 * <p>
 * Lifecycle:
 * 1. TPAP applies with sponsor PSP reference
 * 2. NRB reviews → PENDING_APPROVAL → APPROVED
 * 3. TPAP gets API credentials delegated through sponsor PSP
 * 4. All transactions route via sponsor PSP bank code
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TpapService {

    private final TpapRepository tpapRepository;
    private final PspRepository pspRepository;

    /**
     * Register a new TPAP under a sponsor PSP.
     */
    @Transactional
    public Tpap register(String tpapId, String name, UUID sponsorPspId,
                          String nrbLicenseNumber, LocalDate nrbLicenseExpiry,
                          String contactEmail, String contactPhone) {

        Psp sponsorPsp = pspRepository.findById(sponsorPspId)
                .orElseThrow(() -> new IllegalArgumentException("Sponsor PSP not found: " + sponsorPspId));

        if (!"PRODUCTION".equals(sponsorPsp.getOnboardingStage())) {
            throw new IllegalStateException("Sponsor PSP must be in PRODUCTION stage");
        }

        if (sponsorPsp.getSponsorBankCode() == null && sponsorPsp.getOnboardingStage() != null) {
            // PSP itself can be the bank — use its own bank code from VPA namespace
            log.info("Sponsor PSP {} does not have explicit sponsor bank, using PSP-level routing", sponsorPsp.getPspId());
        }

        Tpap tpap = Tpap.builder()
                .tpapId(tpapId)
                .name(name)
                .sponsorPsp(sponsorPsp)
                .sponsorBankCode(sponsorPsp.getSponsorBankCode() != null
                        ? sponsorPsp.getSponsorBankCode() : "NCHL")
                .status("PENDING_APPROVAL")
                .nrbLicenseNumber(nrbLicenseNumber)
                .nrbLicenseExpiry(nrbLicenseExpiry)
                .technicalContactEmail(contactEmail)
                .technicalContactPhone(contactPhone)
                .build();

        tpap = tpapRepository.save(tpap);
        log.info("TPAP registered: {} ({}) under sponsor PSP {}", tpapId, name, sponsorPsp.getPspId());
        return tpap;
    }

    /**
     * Approve a TPAP application.
     */
    @Transactional
    public Tpap approve(String tpapId) {
        Tpap tpap = tpapRepository.findByTpapId(tpapId)
                .orElseThrow(() -> new IllegalArgumentException("TPAP not found: " + tpapId));

        if (!"PENDING_APPROVAL".equals(tpap.getStatus())) {
            throw new IllegalStateException("TPAP is not in PENDING_APPROVAL state");
        }

        tpap.setStatus("APPROVED");
        tpap.setIsActive(true);
        tpap = tpapRepository.save(tpap);
        log.info("TPAP approved: {} ({})", tpap.getTpapId(), tpap.getName());
        return tpap;
    }

    /**
     * Suspend a TPAP — disables all transactions through this TPAP.
     */
    @Transactional
    public Tpap suspend(String tpapId) {
        Tpap tpap = tpapRepository.findByTpapId(tpapId)
                .orElseThrow(() -> new IllegalArgumentException("TPAP not found: " + tpapId));

        tpap.setStatus("SUSPENDED");
        tpap.setIsActive(false);
        tpap = tpapRepository.save(tpap);
        log.warn("TPAP suspended: {} ({})", tpap.getTpapId(), tpap.getName());
        return tpap;
    }

    /**
     * List all active TPAPs.
     */
    public List<Tpap> listActive() {
        return tpapRepository.findByIsActiveTrue();
    }

    /**
     * List TPAPs under a specific sponsor PSP.
     */
    public List<Tpap> listBySponsorPsp(String pspId) {
        Psp psp = pspRepository.findByPspId(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + pspId));
        return tpapRepository.findBySponsorPspId(psp.getId());
    }

    /**
     * Get TPAP by ID.
     */
    public Tpap getByTpapId(String tpapId) {
        return tpapRepository.findByTpapId(tpapId)
                .orElseThrow(() -> new IllegalArgumentException("TPAP not found: " + tpapId));
    }
}
