package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.MobileRecharge;
import np.com.nepalupi.repository.MobileRechargeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Mobile recharge service for Nepal telecom operators.
 * Supports Ncell, NTC (Nepal Telecom), Smart Cell prepaid/postpaid recharge.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileRechargeService {

    private final MobileRechargeRepository rechargeRepository;

    // Supported operators based on mobile prefix
    private static final Map<String, String> OPERATOR_MAP = Map.ofEntries(
            Map.entry("980", "NTC"), Map.entry("981", "NTC"), Map.entry("982", "NTC"),
            Map.entry("984", "NTC"), Map.entry("985", "NTC"), Map.entry("986", "NTC"),
            Map.entry("974", "NCELL"), Map.entry("975", "NCELL"), Map.entry("976", "NCELL"),
            Map.entry("961", "SMART_CELL"), Map.entry("962", "SMART_CELL"), Map.entry("988", "SMART_CELL")
    );

    /**
     * Initiate a mobile recharge.
     */
    public MobileRecharge initiateRecharge(UUID userId, String mobileNumber, Long amountPaisa,
                                            String rechargeType, String planId) {
        String normalizedMobile = normalizeMobile(mobileNumber);
        String operator = detectOperator(normalizedMobile);

        if (operator == null) {
            throw new IllegalArgumentException("Unsupported mobile operator for number: " + mobileNumber);
        }

        // Validate amount
        if (amountPaisa < 1000) { // Min Rs 10
            throw new IllegalArgumentException("Minimum recharge amount is Rs 10");
        }
        if (amountPaisa > 500000) { // Max Rs 5,000
            throw new IllegalArgumentException("Maximum recharge amount is Rs 5,000");
        }

        MobileRecharge recharge = MobileRecharge.builder()
                .userId(userId)
                .mobileNumber(normalizedMobile)
                .operator(operator)
                .amountPaisa(amountPaisa)
                .rechargeType(rechargeType != null ? rechargeType : "PREPAID")
                .planId(planId)
                .status("INITIATED")
                .build();

        rechargeRepository.save(recharge);
        log.info("Recharge initiated: userId={}, mobile={}, operator={}, amount={}",
                userId, normalizedMobile, operator, amountPaisa);

        // In production: call operator API asynchronously, update status on callback
        processRechargeAsync(recharge);

        return recharge;
    }

    /**
     * Get recharge history for a user.
     */
    public List<MobileRecharge> getHistory(UUID userId) {
        return rechargeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Check recharge status.
     */
    public MobileRecharge getStatus(UUID rechargeId) {
        return rechargeRepository.findById(rechargeId)
                .orElseThrow(() -> new IllegalArgumentException("Recharge not found: " + rechargeId));
    }

    /**
     * Get available plans for an operator.
     */
    public List<Map<String, Object>> getPlans(String operator) {
        // In production: fetch from operator API
        List<Map<String, Object>> plans = new ArrayList<>();

        switch (operator.toUpperCase()) {
            case "NTC" -> {
                plans.add(Map.of("id", "NTC_DATA_1GB", "name", "1GB Data Pack", "amount", 15000, "validity", "7 days"));
                plans.add(Map.of("id", "NTC_DATA_3GB", "name", "3GB Data Pack", "amount", 30000, "validity", "28 days"));
                plans.add(Map.of("id", "NTC_VOICE_100", "name", "100 Min Voice", "amount", 10000, "validity", "28 days"));
                plans.add(Map.of("id", "NTC_COMBO", "name", "Combo Pack", "amount", 50000, "validity", "28 days"));
            }
            case "NCELL" -> {
                plans.add(Map.of("id", "NCELL_DATA_1GB", "name", "1GB Data Pack", "amount", 12000, "validity", "7 days"));
                plans.add(Map.of("id", "NCELL_DATA_5GB", "name", "5GB Data Pack", "amount", 40000, "validity", "28 days"));
                plans.add(Map.of("id", "NCELL_SOCIAL", "name", "Social Media Pack", "amount", 7500, "validity", "7 days"));
            }
            case "SMART_CELL" -> {
                plans.add(Map.of("id", "SMART_DATA_2GB", "name", "2GB Data Pack", "amount", 20000, "validity", "28 days"));
                plans.add(Map.of("id", "SMART_UNLIMITED", "name", "Unlimited Data", "amount", 100000, "validity", "28 days"));
            }
        }
        return plans;
    }

    /**
     * Detect operator from mobile number prefix.
     */
    public String detectOperator(String mobile) {
        if (mobile.length() >= 3) {
            String prefix = mobile.substring(0, 3);
            return OPERATOR_MAP.get(prefix);
        }
        return null;
    }

    private String normalizeMobile(String mobile) {
        mobile = mobile.replaceAll("[^0-9]", "");
        if (mobile.startsWith("977") && mobile.length() == 13) {
            mobile = mobile.substring(3);
        }
        if (mobile.startsWith("0") && mobile.length() == 11) {
            mobile = mobile.substring(1);
        }
        return mobile;
    }

    private void processRechargeAsync(MobileRecharge recharge) {
        // Stub: In production, this would call the telecom operator API
        // For now, simulate success
        recharge.setStatus("COMPLETED");
        recharge.setOperatorTxnRef("OPR-" + UUID.randomUUID().toString().substring(0, 8));
        recharge.setUpdatedAt(Instant.now());
        rechargeRepository.save(recharge);
        log.info("Recharge completed: id={}, operatorRef={}", recharge.getId(), recharge.getOperatorTxnRef());
    }
}
