package np.com.nepalupi.service.dispute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Chargeback;
import np.com.nepalupi.domain.entity.Dispute;
import np.com.nepalupi.repository.ChargebackRepository;
import np.com.nepalupi.repository.DisputeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Chargeback service handling the full chargeback lifecycle:
 * INITIATED → ACQUIRER_NOTIFIED → REPRESENTMENT → ARBITRATION → RESOLVED/REJECTED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChargebackService {

    private final ChargebackRepository chargebackRepository;
    private final DisputeRepository disputeRepository;

    private static final int SLA_DAYS = 45;

    /**
     * Initiate a chargeback from a dispute that hasn't been resolved satisfactorily.
     */
    public Chargeback initiateChargeback(UUID disputeId, Long chargebackAmount, String reason, String initiatedBy) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        // Check if chargeback already exists for this dispute
        chargebackRepository.findByDisputeId(disputeId).ifPresent(cb -> {
            throw new IllegalStateException("Chargeback already exists for dispute: " + disputeId);
        });

        Chargeback chargeback = Chargeback.builder()
                .disputeId(disputeId)
                .transactionId(dispute.getTransactionId())
                .originalAmount(dispute.getTransactionId() != null ? chargebackAmount : 0L)
                .chargebackAmount(chargebackAmount)
                .reason(reason)
                .initiatedBy(initiatedBy)
                .status("INITIATED")
                .slaDeadline(Instant.now().plus(SLA_DAYS, ChronoUnit.DAYS))
                .build();

        chargebackRepository.save(chargeback);
        log.info("Chargeback initiated: disputeId={}, amount={}", disputeId, chargebackAmount);
        return chargeback;
    }

    /**
     * Notify the acquirer bank of the chargeback.
     */
    public Chargeback notifyAcquirer(UUID chargebackId) {
        Chargeback cb = findById(chargebackId);
        cb.setStatus("ACQUIRER_NOTIFIED");
        cb.setUpdatedAt(Instant.now());
        chargebackRepository.save(cb);
        log.info("Acquirer notified for chargeback: {}", chargebackId);
        return cb;
    }

    /**
     * Record acquirer's representment (defense against chargeback).
     */
    public Chargeback recordRepresentment(UUID chargebackId, String evidence) {
        Chargeback cb = findById(chargebackId);
        cb.setStatus("REPRESENTMENT");
        cb.setRepresentmentEvidence(evidence);
        cb.setUpdatedAt(Instant.now());
        chargebackRepository.save(cb);
        log.info("Representment recorded for chargeback: {}", chargebackId);
        return cb;
    }

    /**
     * Escalate to arbitration (NRB/NCHL mediated).
     */
    public Chargeback escalateToArbitration(UUID chargebackId) {
        Chargeback cb = findById(chargebackId);
        cb.setStatus("ARBITRATION");
        cb.setArbitrationStatus("PENDING");
        cb.setUpdatedAt(Instant.now());
        chargebackRepository.save(cb);
        log.info("Chargeback escalated to arbitration: {}", chargebackId);
        return cb;
    }

    /**
     * Resolve the chargeback (in favor of issuer or acquirer).
     */
    public Chargeback resolve(UUID chargebackId, String resolution, String responseFromAcquirer) {
        Chargeback cb = findById(chargebackId);
        cb.setStatus("RESOLVED");
        cb.setResponseFromAcquirer(responseFromAcquirer);
        cb.setResolvedAt(Instant.now());
        cb.setUpdatedAt(Instant.now());
        chargebackRepository.save(cb);
        log.info("Chargeback resolved: {}, resolution={}", chargebackId, resolution);
        return cb;
    }

    /**
     * Reject the chargeback (insufficient evidence).
     */
    public Chargeback reject(UUID chargebackId, String reason) {
        Chargeback cb = findById(chargebackId);
        cb.setStatus("REJECTED");
        cb.setResponseFromAcquirer(reason);
        cb.setResolvedAt(Instant.now());
        cb.setUpdatedAt(Instant.now());
        chargebackRepository.save(cb);
        log.info("Chargeback rejected: {}", chargebackId);
        return cb;
    }

    public Page<Chargeback> getByStatus(String status, Pageable pageable) {
        return chargebackRepository.findByStatus(status, pageable);
    }

    public Optional<Chargeback> getByDispute(UUID disputeId) {
        return chargebackRepository.findByDisputeId(disputeId);
    }

    private Chargeback findById(UUID id) {
        return chargebackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chargeback not found: " + id));
    }
}
