package np.com.nepalupi.service.dispute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Dispute;
import np.com.nepalupi.repository.DisputeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Monitors dispute SLA timers and auto-escalates stale disputes.
 * <p>
 * Never leave a user without an update for more than 24 hours.
 * Auto-escalate to NCHL after 3 days, to NRB after 7 days.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeEscalationService {

    private final DisputeRepository disputeRepo;
    private final DisputeService disputeService;

    /**
     * Check for SLA breaches every hour.
     */
    @Scheduled(fixedRate = 3600000)  // every hour
    public void checkSlaBreaches() {
        List<Dispute> breaches = disputeRepo.findSlaBreaches(Instant.now());

        for (Dispute dispute : breaches) {
            log.warn("SLA BREACH: Dispute {} (case: {}) — deadline was {}",
                    dispute.getId(), dispute.getCaseRef(), dispute.getSlaDeadline());

            // Auto-escalate based on current level
            if (dispute.getEscalationLevel() == 0) {
                disputeService.escalate(dispute.getId(), 1, "system",
                        "SLA breached. Auto-escalating to NCHL dispute desk.");
            } else if (dispute.getEscalationLevel() == 1) {
                disputeService.escalate(dispute.getId(), 2, "system",
                        "NCHL escalation SLA breached. Auto-escalating to NRB.");
            }
        }

        if (!breaches.isEmpty()) {
            log.warn("Total SLA breaches found: {}. Ops team alerted.", breaches.size());
        }
    }

    /**
     * Check for stale disputes (no action for >3 days, not yet escalated).
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kathmandu")  // 9 AM daily
    public void checkStaleDisputes() {
        Instant threeDaysAgo = Instant.now().minusSeconds(3 * 24 * 3600);
        List<Dispute> stale = disputeRepo.findStaleDisputes(threeDaysAgo);

        for (Dispute dispute : stale) {
            log.warn("STALE DISPUTE: {} (case: {}) — no resolution for >3 days",
                    dispute.getId(), dispute.getCaseRef());
            disputeService.escalate(dispute.getId(), 1, "system",
                    "No resolution for >3 days. Auto-escalating.");
        }
    }
}
