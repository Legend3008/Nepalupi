package np.com.nepalupi.mandate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.mandate.entity.CollectBlockList;
import np.com.nepalupi.mandate.entity.CollectSpamTracker;
import np.com.nepalupi.mandate.repository.CollectBlockListRepository;
import np.com.nepalupi.mandate.repository.CollectSpamTrackerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Section 6.2: Collect request anti-spam controls.
 * <p>
 * - Max 10 pending collect requests per payer per day (fraud control)
 * - Block list: users can block unknown collect requestors
 * - Spam scoring based on request patterns
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectAntiSpamService {

    private final CollectSpamTrackerRepository spamTrackerRepository;
    private final CollectBlockListRepository blockListRepository;

    private static final int MAX_COLLECT_PER_PAYER_PER_DAY = 10;
    private static final int MAX_COLLECT_PER_REQUESTOR_TO_PAYER = 3;

    /**
     * Check if a collect request should be allowed.
     *
     * @param requestorVpa VPA of the requestor (payee)
     * @param payerVpa     VPA of the payer (target)
     * @return true if allowed, throws if blocked/spam
     */
    @Transactional
    public boolean validateCollectRequest(String requestorVpa, String payerVpa) {
        // Check block list
        if (blockListRepository.existsByPayerVpaAndBlockedVpa(payerVpa, requestorVpa)) {
            log.warn("Collect request BLOCKED: requestor={} is in payer={}'s block list",
                    requestorVpa, payerVpa);
            throw new IllegalStateException("Collect request blocked by payer");
        }

        // Check per-requestor-to-payer daily limit
        Optional<CollectSpamTracker> tracker = spamTrackerRepository
                .findByRequestorVpaAndTargetPayerVpaAndRequestDate(requestorVpa, payerVpa, LocalDate.now());

        if (tracker.isPresent()) {
            CollectSpamTracker t = tracker.get();
            if (t.getBlocked()) {
                throw new IllegalStateException("Collect requests from this VPA are temporarily blocked");
            }
            if (t.getRequestCount() >= MAX_COLLECT_PER_REQUESTOR_TO_PAYER) {
                log.warn("Collect request SPAM: requestor={} exceeded per-payer daily limit to payer={}",
                        requestorVpa, payerVpa);
                t.setBlocked(true);
                spamTrackerRepository.save(t);
                throw new IllegalStateException("Too many collect requests to this payer today. Max: " +
                        MAX_COLLECT_PER_REQUESTOR_TO_PAYER);
            }
            t.setRequestCount(t.getRequestCount() + 1);
            spamTrackerRepository.save(t);
        } else {
            spamTrackerRepository.save(CollectSpamTracker.builder()
                    .requestorVpa(requestorVpa)
                    .targetPayerVpa(payerVpa)
                    .requestDate(LocalDate.now())
                    .requestCount(1)
                    .blocked(false)
                    .build());
        }

        // Check total daily collect requests received by payer
        long totalToday = spamTrackerRepository.countTotalRequestsToPayerToday(payerVpa, LocalDate.now());
        if (totalToday > MAX_COLLECT_PER_PAYER_PER_DAY) {
            log.warn("Payer {} has received {} collect requests today (limit: {})",
                    payerVpa, totalToday, MAX_COLLECT_PER_PAYER_PER_DAY);
            throw new IllegalStateException("Payer has received too many collect requests today");
        }

        return true;
    }

    /**
     * Block a VPA from sending collect requests to payer.
     */
    @Transactional
    public void blockRequestor(String payerVpa, String blockedVpa, String reason) {
        if (blockListRepository.existsByPayerVpaAndBlockedVpa(payerVpa, blockedVpa)) {
            log.info("VPA {} already blocked by {}", blockedVpa, payerVpa);
            return;
        }

        blockListRepository.save(CollectBlockList.builder()
                .payerVpa(payerVpa)
                .blockedVpa(blockedVpa)
                .reason(reason)
                .build());

        log.info("VPA {} blocked by payer {} (reason: {})", blockedVpa, payerVpa, reason);
    }

    /**
     * Unblock a VPA.
     */
    @Transactional
    public void unblockRequestor(String payerVpa, String blockedVpa) {
        blockListRepository.deleteByPayerVpaAndBlockedVpa(payerVpa, blockedVpa);
        log.info("VPA {} unblocked by payer {}", blockedVpa, payerVpa);
    }

    /**
     * List blocked VPAs for a payer.
     */
    public List<CollectBlockList> getBlockedList(String payerVpa) {
        return blockListRepository.findByPayerVpa(payerVpa);
    }
}
