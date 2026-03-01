package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.DailyTransactionStats;
import np.com.nepalupi.exception.LimitExceededException;
import np.com.nepalupi.repository.DailyTransactionStatsRepository;
import np.com.nepalupi.service.fraud.FraudEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Validates transaction limits per NRB guidelines.
 * <ul>
 *   <li>Per-transaction limit</li>
 *   <li>Daily aggregate amount limit</li>
 *   <li>Daily transaction count limit</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LimitValidationService {

    private final DailyTransactionStatsRepository statsRepo;
    private final FraudEngine fraudEngine;

    @Value("${nepalupi.transaction.per-txn-limit-paisa:10000000}")
    private Long perTransactionLimit;    // Rs 1,00,000

    @Value("${nepalupi.transaction.daily-limit-paisa:20000000}")
    private Long dailyLimit;             // Rs 2,00,000

    @Value("${nepalupi.transaction.daily-count-limit:20}")
    private int dailyCountLimit;

    /**
     * Validate that the requested amount does not breach any limits.
     *
     * @param userId      the user initiating the payment
     * @param amountPaisa the transaction amount in paisa
     * @throws LimitExceededException if any limit is breached
     */
    public void validate(UUID userId, Long amountPaisa) {
        // 1. Per-transaction limit
        if (amountPaisa > perTransactionLimit) {
            throw new LimitExceededException(
                    String.format("Amount %d paisa exceeds per-transaction limit of %d paisa",
                            amountPaisa, perTransactionLimit));
        }

        // 2. Daily aggregate amount & count
        DailyTransactionStats stats = statsRepo
                .findByUserIdAndStatsDate(userId, LocalDate.now())
                .orElse(DailyTransactionStats.builder()
                        .userId(userId)
                        .totalAmountPaisa(0L)
                        .transactionCount(0)
                        .build());

        if (stats.getTotalAmountPaisa() + amountPaisa > dailyLimit) {
            throw new LimitExceededException(
                    String.format("Daily aggregate limit exceeded. Used: %d paisa, Requested: %d paisa, Limit: %d paisa",
                            stats.getTotalAmountPaisa(), amountPaisa, dailyLimit));
        }

        if (stats.getTransactionCount() >= dailyCountLimit) {
            throw new LimitExceededException(
                    String.format("Daily transaction count exceeded. Count: %d, Limit: %d",
                            stats.getTransactionCount(), dailyCountLimit));
        }

        log.debug("Limits validated for user {}: amount={}, dailyTotal={}, dailyCount={}",
                userId, amountPaisa, stats.getTotalAmountPaisa(), stats.getTransactionCount());
    }

    /**
     * Update daily stats after a successful debit.
     */
    public void recordTransaction(UUID userId, Long amountPaisa) {
        DailyTransactionStats stats = statsRepo
                .findByUserIdAndStatsDate(userId, LocalDate.now())
                .orElse(DailyTransactionStats.builder()
                        .userId(userId)
                        .statsDate(LocalDate.now())
                        .totalAmountPaisa(0L)
                        .transactionCount(0)
                        .build());

        stats.setTotalAmountPaisa(stats.getTotalAmountPaisa() + amountPaisa);
        stats.setTransactionCount(stats.getTransactionCount() + 1);
        statsRepo.save(stats);
    }
}
