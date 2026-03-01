package np.com.nepalupi.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.security.entity.VulnerabilityDisclosure;
import np.com.nepalupi.security.enums.DisclosureStatus;
import np.com.nepalupi.security.enums.FindingCategory;
import np.com.nepalupi.security.enums.FindingSeverity;
import np.com.nepalupi.security.repository.VulnerabilityDisclosureRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vulnerability Disclosure / Bug Bounty program management.
 * External researchers report vulnerabilities, we acknowledge within 24h,
 * triage within 7d, fix critical within 30d, and pay bounties.
 *
 * Bounty tiers (NPR paisa):
 * - CRITICAL: Rs 50,000 - Rs 2,00,000
 * - HIGH:     Rs 20,000 - Rs 50,000
 * - MEDIUM:   Rs 5,000  - Rs 20,000
 * - LOW:      Rs 1,000  - Rs 5,000
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BugBountyService {

    private final VulnerabilityDisclosureRepository disclosureRepository;

    @Transactional
    public VulnerabilityDisclosure submitDisclosure(String reporterName, String reporterEmail,
                                                     String reporterAlias, String title,
                                                     String description, FindingSeverity severity,
                                                     FindingCategory category, String affectedSystem,
                                                     String reproductionSteps) {
        VulnerabilityDisclosure disclosure = VulnerabilityDisclosure.builder()
                .reporterName(reporterName)
                .reporterEmail(reporterEmail)
                .reporterAlias(reporterAlias)
                .title(title)
                .description(description)
                .severity(severity)
                .category(category)
                .affectedSystem(affectedSystem)
                .reproductionSteps(reproductionSteps)
                .status(DisclosureStatus.RECEIVED)
                .build();

        disclosure = disclosureRepository.save(disclosure);
        log.info("Bug bounty submission received: id={}, title={}, severity={}", disclosure.getId(), title, severity);
        return disclosure;
    }

    @Transactional
    public VulnerabilityDisclosure acknowledge(UUID disclosureId) {
        VulnerabilityDisclosure d = getOrThrow(disclosureId);
        d.setAcknowledgedAt(Instant.now());
        log.info("Disclosure acknowledged within SLA: id={}", disclosureId);
        return disclosureRepository.save(d);
    }

    @Transactional
    public VulnerabilityDisclosure triage(UUID disclosureId, DisclosureStatus newStatus) {
        VulnerabilityDisclosure d = getOrThrow(disclosureId);
        d.setStatus(newStatus);
        d.setTriagedAt(Instant.now());

        if (newStatus == DisclosureStatus.CONFIRMED) {
            d.setStatus(DisclosureStatus.CONFIRMED);
        } else if (newStatus == DisclosureStatus.DUPLICATE || newStatus == DisclosureStatus.OUT_OF_SCOPE
                || newStatus == DisclosureStatus.INVALID) {
            log.info("Disclosure closed as {}: id={}", newStatus, disclosureId);
        }
        return disclosureRepository.save(d);
    }

    @Transactional
    public VulnerabilityDisclosure markFixed(UUID disclosureId, long bountyAmountPaisa) {
        VulnerabilityDisclosure d = getOrThrow(disclosureId);
        d.setStatus(DisclosureStatus.FIXED);
        d.setFixedAt(Instant.now());
        d.setBountyAmountPaisa(bountyAmountPaisa);
        log.info("Disclosure fixed: id={}, bounty=Rs.{}", disclosureId, bountyAmountPaisa / 100);
        return disclosureRepository.save(d);
    }

    @Transactional
    public VulnerabilityDisclosure payBounty(UUID disclosureId) {
        VulnerabilityDisclosure d = getOrThrow(disclosureId);
        if (d.getBountyAmountPaisa() <= 0) {
            throw new IllegalStateException("No bounty amount set for disclosure: " + disclosureId);
        }
        d.setBountyPaid(true);
        d.setBountyPaidAt(Instant.now());
        log.info("Bounty paid: id={}, amount=Rs.{}, reporter={}", disclosureId,
                d.getBountyAmountPaisa() / 100, d.getReporterAlias());
        return disclosureRepository.save(d);
    }

    public List<VulnerabilityDisclosure> getPendingDisclosures() {
        return disclosureRepository.findByStatusOrderByCreatedAtDesc(DisclosureStatus.RECEIVED);
    }

    public List<VulnerabilityDisclosure> getUnacknowledged() {
        return disclosureRepository.findUnacknowledged();
    }

    /**
     * Hourly check: any disclosure not acknowledged within 24h violates our SLA.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void checkAcknowledgementSla() {
        List<VulnerabilityDisclosure> unacked = disclosureRepository.findUnacknowledged();
        Instant twentyFourHoursAgo = Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
        unacked.stream()
                .filter(d -> d.getCreatedAt().isBefore(twentyFourHoursAgo))
                .forEach(d -> log.error("SLA BREACH: Disclosure id={} not acknowledged within 24h, submitted={}",
                        d.getId(), d.getCreatedAt()));
    }

    private VulnerabilityDisclosure getOrThrow(UUID id) {
        return disclosureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disclosure not found: " + id));
    }
}
