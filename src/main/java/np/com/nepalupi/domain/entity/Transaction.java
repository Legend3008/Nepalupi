package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.domain.enums.TransactionType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * The most critical table in the system.
 * <p>
 * All monetary values in PAISA (NPR × 100). Never use floating point.
 */
@Entity
@Table(name = "transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "upi_txn_id", unique = true, nullable = false)
    private String upiTxnId;

    @Column(name = "rrn", unique = true, nullable = false)
    private String rrn;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false)
    @Builder.Default
    private TransactionType txnType = TransactionType.PAY;

    // ── Parties ──────────────────────────────────────────────
    @Column(name = "payer_vpa", nullable = false)
    private String payerVpa;

    @Column(name = "payee_vpa", nullable = false)
    private String payeeVpa;

    @Column(name = "payer_bank_code", nullable = false)
    private String payerBankCode;

    @Column(name = "payee_bank_code", nullable = false)
    private String payeeBankCode;

    // ── Money (PAISA — never decimal) ────────────────────────
    @Column(nullable = false)
    private Long amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "NPR";

    // ── State machine ────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "failure_code")
    private String failureCode;

    // ── Timing ───────────────────────────────────────────────
    @Column(name = "initiated_at")
    @Builder.Default
    private Instant initiatedAt = Instant.now();

    @Column(name = "debited_at")
    private Instant debitedAt;

    @Column(name = "credited_at")
    private Instant creditedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // ── Audit ────────────────────────────────────────────────
    @Column(name = "psp_id")
    private String pspId;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "ip_address")
    private String ipAddress;

    private String note;

    // ── Idempotency ──────────────────────────────────────────
    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Check if this transaction has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
