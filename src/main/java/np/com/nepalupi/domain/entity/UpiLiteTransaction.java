package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upi_lite_transaction")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpiLiteTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "txn_type", nullable = false)
    private String txnType; // LOAD, PAY, REFUND

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    @Column(name = "payer_vpa")
    private String payerVpa;

    @Column(name = "payee_vpa")
    private String payeeVpa;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private String status = "COMPLETED";

    @Builder.Default
    private Boolean settled = false;

    @Column(name = "settlement_batch_id")
    private String settlementBatchId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
