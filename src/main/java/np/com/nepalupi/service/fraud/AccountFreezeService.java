package np.com.nepalupi.service.fraud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.FraudFlagRepository;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Auto account freeze service.
 * Freezes user accounts when risk score exceeds threshold or multiple fraud flags detected.
 * Supports manual freeze/unfreeze by admin.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountFreezeService {

    private final UserRepository userRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final RiskScoringEngine riskScoringEngine;

    private static final int AUTO_FREEZE_THRESHOLD = 70;  // Risk score threshold
    private static final int MAX_UNREVIEWED_FLAGS = 3;      // Max unreviewed flags before freeze

    /**
     * Check if a user should be auto-frozen based on risk assessment.
     * Called after each fraud flag is created.
     */
    public boolean evaluateAndFreeze(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        // Already frozen
        if (Boolean.TRUE.equals(user.getIsFrozen())) {
            return true;
        }

        // Check unreviewed fraud flags count
        long unreviewedCount = fraudFlagRepository.countByUserIdAndReviewedFalse(userId);
        if (unreviewedCount >= MAX_UNREVIEWED_FLAGS) {
            freezeAccount(user, "AUTO: " + unreviewedCount + " unreviewed fraud flags");
            return true;
        }

        // Check recent risk score
        List<np.com.nepalupi.domain.entity.FraudFlag> recentFlags =
                fraudFlagRepository.findByUserIdAndReviewedFalse(userId);

        for (var flag : recentFlags) {
            RiskScoringEngine.RiskAssessment assessment = riskScoringEngine.computeFromFraudFlag(flag);
            if (assessment.score() >= AUTO_FREEZE_THRESHOLD) {
                freezeAccount(user, "AUTO: High risk score " + assessment.score());
                return true;
            }
        }

        return false;
    }

    /**
     * Manually freeze an account (admin action).
     */
    public void freezeAccount(UUID userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        freezeAccount(user, reason);
    }

    /**
     * Unfreeze an account (admin action only).
     */
    public void unfreezeAccount(UUID userId, String adminId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setIsFrozen(false);
        user.setFrozenAt(null);
        user.setFreezeReason(null);
        userRepository.save(user);

        log.info("Account unfrozen: userId={}, admin={}, reason={}", userId, adminId, reason);
    }

    /**
     * Check if an account is frozen.
     */
    public boolean isFrozen(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getIsFrozen()))
                .orElse(false);
    }

    private void freezeAccount(User user, String reason) {
        user.setIsFrozen(true);
        user.setFrozenAt(Instant.now());
        user.setFreezeReason(reason);
        userRepository.save(user);

        log.warn("ACCOUNT FROZEN: userId={}, reason={}", user.getId(), reason);
        // In production: send notification to user, create ops ticket
    }
}
