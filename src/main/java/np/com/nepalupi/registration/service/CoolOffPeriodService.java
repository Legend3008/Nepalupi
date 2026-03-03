package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Section 5.6: Cool-off period enforcement for new registrations.
 * <p>
 * After initial UPI registration, certain high-risk operations are restricted
 * for a configurable cool-off period:
 * <ul>
 *   <li>First 24 hours: Max transaction NPR 5,000</li>
 *   <li>First 48 hours: No international remittance</li>
 *   <li>First 72 hours: No mandate (auto-pay) creation</li>
 *   <li>First 7 days:   Reduced daily limit (NPR 50,000)</li>
 * </ul>
 * This prevents fraud from freshly-compromised accounts and 
 * aligns with NRB prudential guidelines for new digital payment users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CoolOffPeriodService {

    private final StringRedisTemplate redisTemplate;

    private static final String COOLOFF_PREFIX = "cooloff:registration:";

    @Value("${nepalupi.cooloff.initial-hours:24}")
    private int initialCoolOffHours;

    @Value("${nepalupi.cooloff.international-hours:48}")
    private int internationalCoolOffHours;

    @Value("${nepalupi.cooloff.mandate-hours:72}")
    private int mandateCoolOffHours;

    @Value("${nepalupi.cooloff.full-access-days:7}")
    private int fullAccessDays;

    @Value("${nepalupi.cooloff.initial-txn-limit-paisa:500000}")
    private Long initialTxnLimitPaisa;   // Rs 5,000

    @Value("${nepalupi.cooloff.reduced-daily-limit-paisa:5000000}")
    private Long reducedDailyLimitPaisa; // Rs 50,000

    /**
     * Mark a user as newly registered, starting the cool-off period.
     */
    public void startCoolOff(UUID userId) {
        String key = COOLOFF_PREFIX + userId;
        String registrationTime = Instant.now().toString();
        redisTemplate.opsForValue().set(key, registrationTime, Duration.ofDays(fullAccessDays + 1));
        log.info("Cool-off period started for user={}, expires in {} days", userId, fullAccessDays);
    }

    /**
     * Check if user is within the initial high-restriction cool-off window.
     */
    public boolean isInInitialCoolOff(UUID userId) {
        return isWithinCoolOff(userId, Duration.ofHours(initialCoolOffHours));
    }

    /**
     * Check if international remittance is restricted.
     */
    public boolean isInternationalRestricted(UUID userId) {
        return isWithinCoolOff(userId, Duration.ofHours(internationalCoolOffHours));
    }

    /**
     * Check if mandate creation is restricted.
     */
    public boolean isMandateRestricted(UUID userId) {
        return isWithinCoolOff(userId, Duration.ofHours(mandateCoolOffHours));
    }

    /**
     * Check if the user is still in reduced-limit period.
     */
    public boolean isInReducedLimitPeriod(UUID userId) {
        return isWithinCoolOff(userId, Duration.ofDays(fullAccessDays));
    }

    /**
     * Get the effective per-transaction limit during cool-off.
     * Returns null if user is not in cool-off (normal limits apply).
     */
    public Long getEffectiveTxnLimit(UUID userId) {
        if (isInInitialCoolOff(userId)) {
            return initialTxnLimitPaisa;
        }
        return null; // Normal limits apply
    }

    /**
     * Get the effective daily limit during cool-off.
     * Returns null if user is not in reduced-limit period.
     */
    public Long getEffectiveDailyLimit(UUID userId) {
        if (isInReducedLimitPeriod(userId)) {
            return reducedDailyLimitPaisa;
        }
        return null; // Normal limits apply
    }

    /**
     * Get cool-off status summary for a user.
     */
    public CoolOffStatus getCoolOffStatus(UUID userId) {
        String raw = redisTemplate.opsForValue().get(COOLOFF_PREFIX + userId);
        if (raw == null) {
            return new CoolOffStatus(false, false, false, false, null, null);
        }

        Instant registrationTime = Instant.parse(raw);
        return new CoolOffStatus(
                isInInitialCoolOff(userId),
                isInternationalRestricted(userId),
                isMandateRestricted(userId),
                isInReducedLimitPeriod(userId),
                registrationTime,
                registrationTime.plus(Duration.ofDays(fullAccessDays))
        );
    }

    /**
     * Remove cool-off restrictions (admin override).
     */
    public void removeCoolOff(UUID userId) {
        redisTemplate.delete(COOLOFF_PREFIX + userId);
        log.info("Cool-off period manually removed for user={}", userId);
    }

    private boolean isWithinCoolOff(UUID userId, Duration window) {
        String raw = redisTemplate.opsForValue().get(COOLOFF_PREFIX + userId);
        if (raw == null) {
            return false;
        }
        Instant registrationTime = Instant.parse(raw);
        return Instant.now().isBefore(registrationTime.plus(window));
    }

    /**
     * Cool-off status record.
     */
    public record CoolOffStatus(
            boolean initialRestriction,
            boolean internationalRestricted,
            boolean mandateRestricted,
            boolean reducedLimits,
            Instant registrationTime,
            Instant fullAccessAt
    ) {}
}
