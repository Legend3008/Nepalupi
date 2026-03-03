package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chargebacks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Chargeback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "dispute_id", nullable = false)
    private UUID disputeId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "original_amount", nullable = false)
    private Long originalAmount;

    @Column(name = "chargeback_amount", nullable = false)
    private Long chargebackAmount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "NPR";

    @Column(length = 30)
    @Builder.Default
    private String status = "INITIATED";

    @Column(length = 500)
    private String reason;

    @Column(name = "initiated_by", length = 50)
    private String initiatedBy;

    @Column(name = "response_from_acquirer", length = 1000)
    private String responseFromAcquirer;

    @Column(name = "representment_evidence", columnDefinition = "TEXT")
    private String representmentEvidence;

    @Column(name = "arbitration_status", length = 30)
    private String arbitrationStatus;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
