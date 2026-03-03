package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Saved beneficiary (payee) for quick future payments.
 */
@Entity
@Table(name = "beneficiaries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "beneficiary_vpa"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "beneficiary_vpa", nullable = false)
    private String beneficiaryVpa;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "beneficiary_mobile")
    private String beneficiaryMobile;

    @Column(name = "nick_name")
    private String nickName;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "is_favorite")
    @Builder.Default
    private Boolean isFavorite = false;

    @Column(name = "last_paid_at")
    private Instant lastPaidAt;

    @Column(name = "transaction_count")
    @Builder.Default
    private Integer transactionCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
