package np.com.nepalupi.billpay.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Intent Payment — models the UPI Intent flow for online merchant integration.
 * Merchants generate a upi://pay? deep-link URL that opens the user's PSP app.
 * The user reviews and authorizes with MPIN, and the payment is processed.
 */
@Entity
@Table(name = "intent_payment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IntentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "intent_ref", unique = true, nullable = false)
    private String intentRef;

    @Column(name = "merchant_vpa", nullable = false)
    private String merchantVpa;

    @Column(name = "amount_paisa")
    private Long amountPaisa;

    private String note;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "intent_url", nullable = false, length = 2000)
    private String intentUrl;

    @Column(nullable = false)
    @Builder.Default
    private String status = "CREATED"; // CREATED, AUTHORIZED, COMPLETED, EXPIRED, FAILED

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "payer_vpa")
    private String payerVpa;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
