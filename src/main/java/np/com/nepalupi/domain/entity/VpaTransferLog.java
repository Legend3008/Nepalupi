package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 4: VPA transfer log — tracks when a VPA is moved between bank accounts.
 */
@Entity
@Table(name = "vpa_transfer_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VpaTransferLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vpa_address", nullable = false)
    private String vpaAddress;

    @Column(name = "from_bank_code", nullable = false)
    private String fromBankCode;

    @Column(name = "to_bank_code", nullable = false)
    private String toBankCode;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(name = "transferred_by", nullable = false)
    private UUID transferredBy;

    @Column(name = "transferred_at")
    @Builder.Default
    private Instant transferredAt = Instant.now();
}
