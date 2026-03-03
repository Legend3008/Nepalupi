package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Tax payment entity.
 * Section 19.6: Government tax payments via UPI.
 */
@Entity
@Table(name = "tax_payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TaxPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tax_type", nullable = false)
    private String taxType;  // INCOME_TAX, VAT, TDS, CUSTOM_DUTY, VEHICLE_TAX

    @Column(name = "taxpayer_pan", nullable = false)
    private String taxpayerPan;

    @Column(name = "taxpayer_name", nullable = false)
    private String taxpayerName;

    @Column(name = "fiscal_year", nullable = false)
    private String fiscalYear;  // e.g., '2081/82'

    @Column(name = "tax_period")
    private String taxPeriod;

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    @Column(name = "fine_paisa")
    @Builder.Default
    private Long finePaisa = 0L;

    @Column(name = "interest_paisa")
    @Builder.Default
    private Long interestPaisa = 0L;

    @Column(name = "total_amount_paisa", nullable = false)
    private Long totalAmountPaisa;

    @Column(name = "ird_office_code")
    private String irdOfficeCode;

    @Column(name = "voucher_number")
    private String voucherNumber;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payment_receipt_number")
    private String paymentReceiptNumber;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
