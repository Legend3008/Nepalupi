package np.com.nepalupi.security.repository;

import np.com.nepalupi.security.entity.WafRule;
import np.com.nepalupi.security.enums.WafRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WafRuleRepository extends JpaRepository<WafRule, UUID> {

    List<WafRule> findByIsActiveTrueOrderByPriority();

    List<WafRule> findByRuleTypeOrderByPriority(WafRuleType ruleType);
}
