package np.com.nepalupi.service.fraud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.FraudFlag;
import np.com.nepalupi.repository.FraudFlagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Fraud review service for ops/admin to review, approve, or dismiss fraud flags.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudReviewService {

    private final FraudFlagRepository fraudFlagRepository;
    private final AccountFreezeService accountFreezeService;

    /**
     * Get all unreviewed fraud flags (paginated).
     */
    public Page<FraudFlag> getUnreviewedFlags(Pageable pageable) {
        return fraudFlagRepository.findByReviewedFalse(pageable);
    }

    /**
     * Get fraud flags for a specific user.
     */
    public List<FraudFlag> getFlagsByUser(UUID userId) {
        return fraudFlagRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Mark a fraud flag as reviewed (confirmed fraud).
     * May trigger account freeze if threshold exceeded.
     */
    public FraudFlag confirmFraud(UUID flagId, String reviewerId) {
        FraudFlag flag = fraudFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud flag not found: " + flagId));

        flag.setReviewed(true);
        fraudFlagRepository.save(flag);

        // Re-evaluate freeze after confirming
        accountFreezeService.evaluateAndFreeze(flag.getUserId());

        log.info("Fraud flag confirmed: flagId={}, reviewerId={}, userId={}", flagId, reviewerId, flag.getUserId());
        return flag;
    }

    /**
     * Dismiss a fraud flag (false positive).
     */
    public FraudFlag dismissFlag(UUID flagId, String reviewerId) {
        FraudFlag flag = fraudFlagRepository.findById(flagId)
                .orElseThrow(() -> new IllegalArgumentException("Fraud flag not found: " + flagId));

        flag.setReviewed(true);
        fraudFlagRepository.save(flag);

        log.info("Fraud flag dismissed: flagId={}, reviewerId={}, userId={}", flagId, reviewerId, flag.getUserId());
        return flag;
    }

    /**
     * Get flags for a specific transaction.
     */
    public List<FraudFlag> getFlagsByTransaction(UUID transactionId) {
        return fraudFlagRepository.findByTransactionId(transactionId);
    }

    /**
     * Get summary stats for fraud review dashboard.
     */
    public Map<String, Object> getReviewDashboard() {
        long totalUnreviewed = fraudFlagRepository.findByReviewedFalseOrderByCreatedAtDesc().size();
        long totalFlags = fraudFlagRepository.count();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalUnreviewed", totalUnreviewed);
        dashboard.put("totalFlags", totalFlags);
        dashboard.put("reviewedPercentage", totalFlags > 0 ? ((totalFlags - totalUnreviewed) * 100.0 / totalFlags) : 100.0);
        return dashboard;
    }
}
