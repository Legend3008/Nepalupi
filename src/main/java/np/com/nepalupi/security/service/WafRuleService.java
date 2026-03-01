package np.com.nepalupi.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.security.entity.WafRule;
import np.com.nepalupi.security.enums.WafAction;
import np.com.nepalupi.security.enums.WafRuleType;
import np.com.nepalupi.security.repository.WafRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * WAF rule management. The WAF operates in BLOCKING mode — every
 * blocked request is logged with full details for analysis.
 * Rules cover: SQL injection, XSS, request smuggling, rate limits,
 * geo-blocking, and bot detection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WafRuleService {

    private final WafRuleRepository wafRuleRepository;

    @Transactional
    public WafRule createRule(String ruleName, WafRuleType ruleType, WafAction action,
                              String pattern, String description, int priority) {
        WafRule rule = WafRule.builder()
                .ruleName(ruleName)
                .ruleType(ruleType)
                .action(action)
                .pattern(pattern)
                .description(description)
                .priority(priority)
                .isActive(true)
                .build();

        rule = wafRuleRepository.save(rule);
        log.info("WAF rule created: name={}, type={}, action={}, priority={}", ruleName, ruleType, action, priority);
        return rule;
    }

    @Transactional
    public WafRule toggleRule(UUID ruleId, boolean active) {
        WafRule rule = wafRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("WAF rule not found"));
        rule.setIsActive(active);
        log.info("WAF rule {}: name={}", active ? "activated" : "deactivated", rule.getRuleName());
        return wafRuleRepository.save(rule);
    }

    @Transactional
    public WafRule recordHit(UUID ruleId) {
        WafRule rule = wafRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("WAF rule not found"));
        rule.setHitsTotal(rule.getHitsTotal() + 1);
        rule.setLastHitAt(Instant.now());
        return wafRuleRepository.save(rule);
    }

    public List<WafRule> getActiveRules() {
        return wafRuleRepository.findByIsActiveTrueOrderByPriority();
    }

    public List<WafRule> getRulesByType(WafRuleType type) {
        return wafRuleRepository.findByRuleTypeOrderByPriority(type);
    }
}
