package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mobile_recharges")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MobileRecharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    @Column(name = "recharge_type", length = 20)
    @Builder.Default
    private String rechargeType = "PREPAID";

    @Column(name = "plan_id", length = 50)
    private String planId;

    @Column(name = "plan_description", length = 255)
    private String planDescription;

    @Column(length = 30)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "operator_txn_ref", length = 100)
    private String operatorTxnRef;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
