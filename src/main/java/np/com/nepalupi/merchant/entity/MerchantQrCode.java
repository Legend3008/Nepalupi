package np.com.nepalupi.merchant.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.merchant.enums.QrCodeType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_qr_code")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantQrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_type", nullable = false)
    private QrCodeType qrType = QrCodeType.STATIC;

    @Column(name = "qr_data", nullable = false, columnDefinition = "TEXT")
    private String qrData;

    @Column(name = "qr_payload", columnDefinition = "jsonb")
    private String qrPayload;

    @Column(name = "amount_paisa")
    private Long amountPaisa;

    @Column(name = "merchant_txn_ref")
    private String merchantTxnRef;

    private String description;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    private Boolean scanned = false;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
