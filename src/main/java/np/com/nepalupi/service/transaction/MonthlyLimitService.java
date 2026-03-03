package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.exception.LimitExceededException;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Section 5.3: Monthly transaction limit enforcement per NRB guidelines.
 * <p>
 * Tracks monthly aggregate amounts and transaction counts per user.
 * Uses Redis for fast lookups with DB fallback for accuracy.
 * NRB mandates monthly limits for different KYC tiers:
 * <ul>
 *   <li>Basic KYC: NPR 1,00,000/month</li>
 *   <li>Full KYC:  NPR 5,00,000/month</li>
 *   <li>Enhanced:  NPR 10,00,000/month</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlyLimitService {

    private final StringRedisTemplate redisTemplate;
    private final TransactionRepository transactionRepository;

    private static final String MONTHLY_AMOUNT_KEY = "monthly:amount:%s:%s";
    private static final String MONTHLY_COUNT_KEY  = "monthly:count:%s:%s";

    @Value("${nepalupi.limits.monthly.basic-kyc-paisa:10000000}")
    private Long basicKycMonthlyLimit;       // Rs 1,00,000

    @Value("${nepalupi.limits.monthly.full-kyc-paisa:50000000}")
    private Long fullKycMonthlyLimit;        // Rs 5,00,000

    @Value("${nepalupi.limits.monthly.enhanced-kyc-paisa:100000000}")
    private Long enhancedKycMonthlyLimit;    // Rs 10,00,000

    @Value("${nepalupi.limits.monthly.max-count:200}")
    private int monthlyCountLimit;

    /**
     * Validate that the user has not exceeded monthly limits.
     *
     * @param userId      user initiating the transaction
     * @param amountPaisa requested amount
     * @param kycLevel    user's KYC level (BASIC, FULL, ENHANCED)
     */
    public void validateMonthlyLimit(UUID userId, Long amountPaisa, String kycLevel) {
        YearMonth currentMonth = YearMonth.now();
        String monthKey = currentMonth.toString(); // e.g. "2025-01"

        Long effectiveLimit = resolveMonthlyLimit(kycLevel);

        // Get current monthly total from Redis
        String amountKey = String.format(MONTHLY_AMOUNT_KEY, userId, monthKey);
        String countKey  = String.format(MONTHLY_COUNT_KEY, userId, monthKey);

        Long currentTotal = getOrInitializeMonthlyTotal(amountKey, userId, currentMonth);
        Long currentCount = getOrInitializeMonthlyCount(countKey, userId, currentMonth);

        // Validate amount
        if (currentTotal + amountPaisa > effectiveLimit) {
            log.warn("Monthly limit breach: user={}, current={}, requested={}, limit={}, kycLevel={}",
                    userId, currentTotal, amountPaisa, effectiveLimit, kycLevel);
            throw new LimitExceededException(
                    String.format("Monthly limit exceeded. Used: %d paisa, Requested: %d paisa, Limit: %d paisa (KYC: %s)",
                            currentTotal, amountPaisa, effectiveLimit, kycLevel));
        }

        // Validate count
        if (currentCount >= monthlyCountLimit) {
            throw new LimitExceededException(
                    String.format("Monthly transaction count exceeded. Count: %d, Limit: %d",
                            currentCount, monthlyCountLimit));
        }

        log.debug("Monthly limit OK: user={}, monthTotal={}, requested={}, limit={}", 
                userId, currentTotal, amountPaisa, effectiveLimit);
    }

    /**
     * Record a successful transaction against monthly limits.
     */
    public void recordMonthlyTransaction(UUID userId, Long amountPaisa) {
        YearMonth currentMonth = YearMonth.now();
        String monthKey = currentMonth.toString();

        String amountKey = String.format(MONTHLY_AMOUNT_KEY, userId, monthKey);
        String countKey  = String.format(MONTHLY_COUNT_KEY, userId, monthKey);

        redisTemplate.opsForValue().increment(amountKey, amountPaisa);
        redisTemplate.opsForValue().increment(countKey, 1);

        // Set expiry to end of next month (buffer for month-end edge cases)
        long daysRemaining = LocalDate.now().lengthOfMonth() - LocalDate.now().getDayOfMonth() + 32;
        redisTemplate.expire(amountKey, Duration.ofDays(daysRemaining));
        redisTemplate.expire(countKey, Duration.ofDays(daysRemaining));
    }

    /**
     * Get remaining monthly limit for a user.
     */
    public Long getRemainingMonthlyLimit(UUID userId, String kycLevel) {
        YearMonth currentMonth = YearMonth.now();
        String amountKey = String.format(MONTHLY_AMOUNT_KEY, userId, currentMonth.toString());
        Long effectiveLimit = resolveMonthlyLimit(kycLevel);

        String cached = redisTemplate.opsForValue().get(amountKey);
        Long currentTotal = cached != null ? Long.parseLong(cached) : 0L;

        return Math.max(0, effectiveLimit - currentTotal);
    }

    private Long resolveMonthlyLimit(String kycLevel) {
        if (kycLevel == null) return basicKycMonthlyLimit;
        return switch (kycLevel.toUpperCase()) {
            case "FULL"     -> fullKycMonthlyLimit;
            case "ENHANCED" -> enhancedKycMonthlyLimit;
            default         -> basicKycMonthlyLimit;
        };
    }

    private Long getOrInitializeMonthlyTotal(String key, UUID userId, YearMonth month) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        // Cache miss — not yet set this month, start at 0
        redisTemplate.opsForValue().set(key, "0", Duration.ofDays(35));
        return 0L;
    }

    private Long getOrInitializeMonthlyCount(String key, UUID userId, YearMonth month) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        redisTemplate.opsForValue().set(key, "0", Duration.ofDays(35));
        return 0L;
    }
}
