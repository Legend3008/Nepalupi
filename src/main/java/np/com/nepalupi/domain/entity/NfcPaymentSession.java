package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nfc_payment_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NfcPaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "merchant_vpa", length = 100)
    private String merchantVpa;

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    @Column(name = "nfc_tag_data", columnDefinition = "TEXT")
    private String nfcTagData;

    @Column(name = "card_emulation_mode", length = 20)
    private String cardEmulationMode;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Column(length = 30)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
