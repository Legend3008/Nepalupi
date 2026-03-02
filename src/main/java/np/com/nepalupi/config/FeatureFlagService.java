package np.com.nepalupi.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.FeatureFlag;
import np.com.nepalupi.repository.FeatureFlagRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Section 20: Feature flag service.
 * <p>
 * Provides kill switches and feature toggles for:
 * - UPI Lite (Section 19.1)
 * - International remittance (Section 19.2)
 * - ML fraud scoring (Section 9.6)
 * - Collect anti-spam (Section 6.2)
 * - Mandate pre-debit notifications (Section 7.2.1)
 * <p>
 * Flags are stored in database and cached in-memory with 1-minute refresh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final Map<String, Boolean> flagCache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        refreshFlags();
        log.info("Feature flags loaded: {} flags, {} enabled",
                flagCache.size(), flagCache.values().stream().filter(v -> v).count());
    }

    /**
     * Check if a feature flag is enabled.
     */
    public boolean isEnabled(String flagKey) {
        return flagCache.getOrDefault(flagKey, false);
    }

    /**
     * Check if a feature flag is enabled with a default value.
     */
    public boolean isEnabled(String flagKey, boolean defaultValue) {
        return flagCache.getOrDefault(flagKey, defaultValue);
    }

    /**
     * Update a feature flag.
     */
    public void setFlag(String flagKey, boolean value, String updatedBy) {
        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature flag: " + flagKey));

        flag.setFlagValue(value);
        flag.setUpdatedBy(updatedBy);
        featureFlagRepository.save(flag);

        flagCache.put(flagKey, value);
        log.info("Feature flag updated: {}={} by {}", flagKey, value, updatedBy);
    }

    /**
     * Get all feature flags and their states.
     */
    public Map<String, Boolean> getAllFlags() {
        return Map.copyOf(flagCache);
    }

    /**
     * Refresh flags from database every 60 seconds.
     */
    @Scheduled(fixedRate = 60_000)
    public void refreshFlags() {
        try {
            featureFlagRepository.findAll().forEach(flag ->
                    flagCache.put(flag.getFlagKey(), flag.getFlagValue()));
        } catch (Exception e) {
            log.warn("Failed to refresh feature flags: {}", e.getMessage());
        }
    }

    // ── Convenience methods for common flags ──

    public boolean isUpiLiteEnabled() {
        return isEnabled("upi.lite.enabled");
    }

    public boolean isInternationalEnabled() {
        return isEnabled("upi.international.enabled");
    }

    public boolean isCreditLineEnabled() {
        return isEnabled("upi.credit_line.enabled");
    }

    public boolean isSignedQrEnabled() {
        return isEnabled("upi.signed_qr.enabled");
    }

    public boolean isCollectAntiSpamEnabled() {
        return isEnabled("collect.anti_spam.enabled", true);
    }

    public boolean isMandatePreDebitNotificationEnabled() {
        return isEnabled("mandate.pre_debit_notification.enabled", true);
    }

    public boolean isIpWhitelistEnabled() {
        return isEnabled("security.ip_whitelist.enabled");
    }

    public boolean isMlFraudScoringEnabled() {
        return isEnabled("fraud.ml_scoring.enabled");
    }
}
