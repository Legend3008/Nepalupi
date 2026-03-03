package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ivr_payment_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IvrPaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "caller_mobile", nullable = false, length = 15)
    private String callerMobile;

    @Column(name = "callee_vpa", length = 100)
    private String calleeVpa;

    @Column(name = "amount_paisa", nullable = false)
    private Long amountPaisa;

    @Column(name = "ivr_session_id", length = 100)
    private String ivrSessionId;

    @Column(name = "dtmf_input", length = 50)
    private String dtmfInput;

    @Column(length = 30)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(length = 10)
    @Builder.Default
    private String language = "ne";

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
