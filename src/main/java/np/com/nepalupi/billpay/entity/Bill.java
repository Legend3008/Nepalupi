package np.com.nepalupi.billpay.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Bill — a specific bill fetched or created for a customer from a registered biller.
 * Supports the BBPS fetch-and-pay model: fetch outstanding bill → pay → confirmation.
 */
@Entity
@Table(name = "bill")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_id", nullable = false)
    private Biller biller;

    @Column(name = "customer_identifier", nullable = false)
    private String customerIdentifier; // account number, consumer number, etc.

    @Column(name = "bill_number")
    private String billNumber;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, PAID, EXPIRED, FAILED

    @Column(name = "payer_vpa")
    private String payerVpa;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "paid_at")
    private Instant paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
