package np.com.nepalupi.mandate.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.mandate.enums.CollectRequestStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "collect_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollectRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "collect_ref", unique = true, nullable = false)
    private String collectRef;

    @Column(name = "requestor_vpa", nullable = false)
    private String requestorVpa;

    @Column(name = "payer_vpa", nullable = false)
    private String payerVpa;

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectRequestStatus status = CollectRequestStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "requestor_psp_id")
    private String requestorPspId;

    @Column(name = "payer_psp_id")
    private String payerPspId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
