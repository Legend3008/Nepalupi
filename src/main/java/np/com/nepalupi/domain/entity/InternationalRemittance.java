package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "international_remittance")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class InternationalRemittance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_country", nullable = false)
    @Builder.Default
    private String sourceCountry = "NPL";

    @Column(name = "dest_country", nullable = false)
    private String destCountry;

    @Column(name = "source_currency", nullable = false)
    @Builder.Default
    private String sourceCurrency = "NPR";

    @Column(name = "dest_currency", nullable = false)
    private String destCurrency;

    @Column(name = "source_amount_minor", nullable = false)
    private Long sourceAmountMinor;

    @Column(name = "dest_amount_minor", nullable = false)
    private Long destAmountMinor;

    @Column(name = "exchange_rate", nullable = false, precision = 15, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "payer_vpa", nullable = false)
    private String payerVpa;

    @Column(name = "payee_identifier", nullable = false)
    private String payeeIdentifier;

    @Column(name = "partner_system", nullable = false)
    private String partnerSystem;

    @Column(nullable = false)
    @Builder.Default
    private String status = "INITIATED";

    @Column(name = "compliance_check_status")
    @Builder.Default
    private String complianceCheckStatus = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
