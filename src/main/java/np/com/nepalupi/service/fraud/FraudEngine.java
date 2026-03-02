package np.com.nepalupi.service.fraud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.FraudFlag;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.domain.enums.FraudSignal;
import np.com.nepalupi.repository.FraudFlagRepository;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.repository.VpaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Real-time fraud assessment engine with 5 active detection rules.
 * <p>
 * Runs synchronously within the transaction flow.
 * Does NOT block transactions — flags for ops review and adds friction
 * (extra auth steps) when suspicious signals are detected.
 * <p>
 * Detection rules:
 * 1. Amount spike (> 5x 30-day average)
 * 2. High velocity (> 5 txns/hour)
 * 3. New device + high amount (> Rs 10,000 from unregistered device)
 * 4. New payee VPA (< 24 hours old)
 * 5. Rapid successive transactions (> 3 within 5 minutes)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudEngine {

    private final TransactionRepository txnRepo;
    private final FraudFlagRepository fraudFlagRepository;
    private final VpaRepository vpaRepository;
    private final ObjectMapper objectMapper;

    /**
     * Assess fraud signals for a transaction.
     * If 2+ signals are triggered, the transaction is flagged for review.
     *
     * @param userId        the payer's user ID
     * @param amountPaisa   transaction amount in paisa
     * @param transactionId the transaction UUID
     * @return list of detected fraud signals
     */
    public List<FraudSignal> assess(UUID userId, Long amountPaisa, UUID transactionId) {
        return assess(userId, amountPaisa, transactionId, null, null);
    }

    /**
     * Enhanced assessment with device fingerprint and payee VPA context.
     */
    public List<FraudSignal> assess(UUID userId, Long amountPaisa, UUID transactionId,
                                     String deviceFingerprint, String payeeVpa) {
        List<FraudSignal> signals = new ArrayList<>();

        // Signal 1: Unusual amount spike (> 5x 30-day average)
        checkAmountSpike(userId, amountPaisa, signals);

        // Signal 2: High velocity (> 5 transactions in last hour)
        checkHighVelocity(userId, signals);

        // Signal 3: New device + high amount (> Rs 10,000)
        checkNewDeviceHighAmount(userId, amountPaisa, deviceFingerprint, signals);

        // Signal 4: New payee VPA (registered < 24 hours ago)
        checkNewPayeeVpa(payeeVpa, signals);

        // Signal 5: Rapid successive (> 3 transactions within 5 minutes)
        checkRapidSuccessive(userId, signals);

        // Flag for review if 2+ signals
        if (signals.size() >= 2) {
            flagForReview(userId, transactionId, signals);
        } else if (!signals.isEmpty()) {
            log.info("Fraud signal detected but below threshold: user={}, signals={}", userId, signals);
        }

        return signals;
    }

    // ── Rule 1: Amount Spike ──

    private void checkAmountSpike(UUID userId, Long amountPaisa, List<FraudSignal> signals) {
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
    }

    // ── Rule 2: High Velocity ──

    private void checkHighVelocity(UUID userId, List<FraudSignal> signals) {
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
    }

    // ── Rule 3: New Device + High Amount ──

    private void checkNewDeviceHighAmount(UUID userId, Long amountPaisa,
                                           String deviceFingerprint, List<FraudSignal> signals) {
        if (deviceFingerprint == null || amountPaisa <= 10_000_00L) {
            return; // Skip if no fingerprint or amount < Rs 10,000
        }

        try {
            // Check if this device fingerprint has been used in previous transactions
            boolean knownDevice = txnRepo.findAll().stream()
                    .filter(t -> t.getDeviceFingerprint() != null
                            && t.getDeviceFingerprint().equals(deviceFingerprint)
                            && t.getCreatedAt() != null
                            && t.getCreatedAt().isBefore(Instant.now().minus(1, ChronoUnit.HOURS)))
                    .findAny()
                    .isPresent();

            if (!knownDevice) {
                signals.add(FraudSignal.NEW_DEVICE_HIGH_AMOUNT);
                log.warn("Fraud signal NEW_DEVICE_HIGH_AMOUNT: user={}, amount={}, device={}",
                        userId, amountPaisa, deviceFingerprint.substring(0, Math.min(8, deviceFingerprint.length())));
            }
        } catch (Exception e) {
            log.debug("Could not check device history for user {}: {}", userId, e.getMessage());
        }
    }

    // ── Rule 4: New Payee VPA ──

    private void checkNewPayeeVpa(String payeeVpa, List<FraudSignal> signals) {
        if (payeeVpa == null) return;

        try {
            vpaRepository.findByVpaAddress(payeeVpa).ifPresent(vpa -> {
                if (vpa.getCreatedAt() != null
                        && vpa.getCreatedAt().isAfter(Instant.now().minus(24, ChronoUnit.HOURS))) {
                    signals.add(FraudSignal.NEW_PAYEE_VPA);
                    log.warn("Fraud signal NEW_PAYEE_VPA: payee={}, createdAt={}",
                            payeeVpa, vpa.getCreatedAt());
                }
            });
        } catch (Exception e) {
            log.debug("Could not check payee VPA age for {}: {}", payeeVpa, e.getMessage());
        }
    }

    // ── Rule 5: Rapid Successive Transactions ──

    private void checkRapidSuccessive(UUID userId, List<FraudSignal> signals) {
        try {
            Instant fiveMinAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
            int recentCount = txnRepo.countTransactionsSince(userId, fiveMinAgo);
            if (recentCount > 3) {
                signals.add(FraudSignal.HIGH_VELOCITY); // Same category, more aggressive window
                log.warn("Fraud signal RAPID_SUCCESSIVE: user={}, txns-in-5min={}", userId, recentCount);
            }
        } catch (Exception e) {
            log.debug("Could not check rapid successive for user {}: {}", userId, e.getMessage());
        }
    }

    // ── Persistence ──

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
