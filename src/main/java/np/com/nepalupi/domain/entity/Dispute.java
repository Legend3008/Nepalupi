package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "raised_by_vpa", nullable = false)
    private String raisedByVpa;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    @Builder.Default
    private String status = "RAISED";

    private String resolution;

    // ── Enhanced dispute fields (V2 migration) ───────────────

    /** DEBIT_WITHOUT_CREDIT / FAILED_BUT_DEBITED / UNAUTHORIZED_TRANSACTION / DUPLICATE_CHARGE / OTHER */
    @Column(name = "dispute_type", length = 50)
    @Builder.Default
    private String disputeType = "DEBIT_WITHOUT_CREDIT";

    /** Unique case reference for user communication (e.g., "DSP-20260302-001") */
    @Column(name = "case_ref", unique = true, length = 30)
    private String caseRef;

    /** SLA deadline — when this dispute must be resolved by */
    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "payer_bank_code", length = 20)
    private String payerBankCode;

    @Column(name = "payee_bank_code", length = 20)
    private String payeeBankCode;

    @Column(name = "amount_paisa")
    private Long amountPaisa;

    /** Transaction ID of the refund, if money was returned */
    @Column(name = "refund_txn_id")
    private UUID refundTxnId;

    /** Whether this dispute was resolved automatically (no human intervention) */
    @Column(name = "auto_resolved")
    @Builder.Default
    private Boolean autoResolved = false;

    /** 0=none, 1=NCHL-escalated, 2=NRB-escalated */
    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    @Column(name = "bank_query_sent_at")
    private Instant bankQuerySentAt;

    @Column(name = "bank_response_at")
    private Instant bankResponseAt;

    @Column(name = "bank_response_details", columnDefinition = "TEXT")
    private String bankResponseDetails;

    // ── Timestamps ───────────────────────────────────────────

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
