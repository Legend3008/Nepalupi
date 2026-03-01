package np.com.nepalupi.security.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.security.enums.WafAction;
import np.com.nepalupi.security.enums.WafRuleType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "waf_rule")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WafRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private WafRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "action", nullable = false)
    private WafAction action = WafAction.BLOCK;

    @Column(name = "pattern", columnDefinition = "TEXT")
    private String pattern;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "priority")
    private Integer priority = 100;

    @Builder.Default
    @Column(name = "hits_total")
    private Long hitsTotal = 0L;

    @Column(name = "last_hit_at")
    private Instant lastHitAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
