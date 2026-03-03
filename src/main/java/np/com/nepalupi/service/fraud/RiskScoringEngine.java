package np.com.nepalupi.service.fraud;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.FraudFlag;
import np.com.nepalupi.domain.enums.FraudSignal;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Numerical risk scoring engine.
 * Computes a weighted risk score (0-100) based on fraud signals.
 * Score ≥ 70 = HIGH RISK (block), 40-69 = MEDIUM (review), < 40 = LOW (pass).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoringEngine {

    /**
     * Weighted risk signal scores.
     */
    private static final Map<FraudSignal, Integer> SIGNAL_WEIGHTS = Map.of(
            FraudSignal.AMOUNT_SPIKE, 25,
            FraudSignal.HIGH_VELOCITY, 20,
            FraudSignal.NEW_DEVICE_HIGH_AMOUNT, 30,
            FraudSignal.RAPID_SUCCESSIVE, 15,
            FraudSignal.CIRCULAR_TRANSACTION, 35,
            FraudSignal.SANCTIONS_HIT, 50,
            FraudSignal.STRUCTURING, 40
    );

    /**
     * Compute risk score for a set of fraud signals.
     *
     * @param signals Set of detected fraud signals
     * @return Risk assessment with score, level, and recommendation
     */
    public RiskAssessment computeRiskScore(Set<FraudSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return new RiskAssessment(0, RiskLevel.LOW, "ALLOW", List.of());
        }

        int totalScore = 0;
        List<String> reasons = new ArrayList<>();

        for (FraudSignal signal : signals) {
            int weight = SIGNAL_WEIGHTS.getOrDefault(signal, 10);
            totalScore += weight;
            reasons.add(signal.name() + " (+" + weight + ")");
        }

        // Cap at 100
        totalScore = Math.min(totalScore, 100);

        RiskLevel level;
        String recommendation;

        if (totalScore >= 70) {
            level = RiskLevel.HIGH;
            recommendation = "BLOCK";
        } else if (totalScore >= 40) {
            level = RiskLevel.MEDIUM;
            recommendation = "REVIEW";
        } else {
            level = RiskLevel.LOW;
            recommendation = "ALLOW";
        }

        log.info("Risk score computed: {} ({}) — signals: {}", totalScore, level, signals);
        return new RiskAssessment(totalScore, level, recommendation, reasons);
    }

    /**
     * Compute risk score from a FraudFlag entity (stored signals JSON).
     */
    public RiskAssessment computeFromFraudFlag(FraudFlag flag) {
        Set<FraudSignal> signals = parseSignals(flag.getSignals());
        return computeRiskScore(signals);
    }

    private Set<FraudSignal> parseSignals(String signalsJson) {
        Set<FraudSignal> signals = new HashSet<>();
        if (signalsJson == null) return signals;

        // Parse JSON array: ["AMOUNT_SPIKE","HIGH_VELOCITY"]
        String cleaned = signalsJson.replaceAll("[\\[\\]\"\\s]", "");
        for (String s : cleaned.split(",")) {
            try {
                signals.add(FraudSignal.valueOf(s.trim()));
            } catch (Exception e) {
                log.warn("Unknown fraud signal: {}", s);
            }
        }
        return signals;
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    public record RiskAssessment(
            int score,
            RiskLevel level,
            String recommendation,
            List<String> scoringDetails
    ) {}
}
