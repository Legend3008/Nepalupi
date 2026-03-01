package np.com.nepalupi.service.fraud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.FraudFlag;
import np.com.nepalupi.domain.enums.FraudSignal;
import np.com.nepalupi.repository.FraudFlagRepository;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Real-time fraud assessment engine.
 * <p>
 * Runs synchronously within the transaction flow.
 * Does NOT block transactions — flags for ops review and adds friction
 * (extra auth steps) when suspicious signals are detected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEngine {

    private final TransactionRepository txnRepo;
    private final FraudFlagRepository fraudFlagRepository;
    private final ObjectMapper objectMapper;

    /**
     * Assess fraud signals for a transaction.
     * If 2+ signals are triggered, the transaction is flagged for review.
     */
    public List<FraudSignal> assess(UUID userId, Long amountPaisa, UUID transactionId) {
        List<FraudSignal> signals = new ArrayList<>();

        // Signal 1: Unusual amount spike (> 5x 30-day average)
        try {
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
            Double avgAmount = txnRepo.getAverageAmount(userId, thirtyDaysAgo);
            if (avgAmount != null && avgAmount > 0 && amountPaisa > avgAmount * 5) {
                signals.add(FraudSignal.AMOUNT_SPIKE);
                log.warn("Fraud signal AMOUNT_SPIKE: user={}, amount={}, avg={}",
                        userId, amountPaisa, avgAmount);
            }
        } catch (Exception e) {
            log.debug("Could not compute average amount for user {}: {}", userId, e.getMessage());
        }

        // Signal 2: High velocity (> 5 transactions in last hour)
        try {
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            int recentCount = txnRepo.countTransactionsSince(userId, oneHourAgo);
            if (recentCount > 5) {
                signals.add(FraudSignal.HIGH_VELOCITY);
                log.warn("Fraud signal HIGH_VELOCITY: user={}, count={}", userId, recentCount);
            }
        } catch (Exception e) {
            log.debug("Could not compute velocity for user {}: {}", userId, e.getMessage());
        }

        // Signal 3: High value from new device (placeholder — needs device tracking)
        // In production: check device fingerprint registry
        if (amountPaisa > 10_000_00L) {
            // signals.add(FraudSignal.NEW_DEVICE_HIGH_AMOUNT);
            // Only add when device tracking is active
        }

        // Flag for review if 2+ signals
        if (signals.size() >= 2) {
            flagForReview(userId, transactionId, signals);
        }

        return signals;
    }

    private void flagForReview(UUID userId, UUID transactionId, List<FraudSignal> signals) {
        log.warn("FRAUD FLAG: user={}, txn={}, signals={}", userId, transactionId, signals);

        String signalsJson;
        try {
            signalsJson = objectMapper.writeValueAsString(signals);
        } catch (JsonProcessingException e) {
            signalsJson = signals.toString();
        }

        FraudFlag flag = FraudFlag.builder()
                .userId(userId)
                .transactionId(transactionId)
                .signals(signalsJson)
                .reviewed(false)
                .createdAt(Instant.now())
                .build();

        fraudFlagRepository.save(flag);
    }
}
