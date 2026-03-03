package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * IPO payment application entity.
 * Section 19.5: IPO/FPO share application via UPI.
 */
@Entity
@Table(name = "ipo_payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IpoPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ipo_name", nullable = false)
    private String ipoName;

    @Column(name = "ipo_code", nullable = false)
    private String ipoCode;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "kitta_applied", nullable = false)
    private Integer kittaApplied;

    @Column(name = "amount_per_kitta_paisa", nullable = false)
    private Long amountPerKittaPaisa;

    @Column(name = "total_amount_paisa", nullable = false)
    private Long totalAmountPaisa;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "demat_number", nullable = false)
    private String dematNumber;

    @Column(name = "boid", nullable = false)
    private String boid;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "application_number")
    private String applicationNumber;

    @Column(name = "applied_at")
    @Builder.Default
    private Instant appliedAt = Instant.now();

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "allotment_kitta")
    private Integer allotmentKitta;

    @Column(name = "refund_amount_paisa")
    @Builder.Default
    private Long refundAmountPaisa = 0L;

    @Column(name = "refund_at")
    private Instant refundAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
