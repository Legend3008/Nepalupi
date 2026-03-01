package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_audit_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransactionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @Column(name = "changed_at")
    @Builder.Default
    private Instant changedAt = Instant.now();

    @Column(columnDefinition = "jsonb")
    private String metadata;
}
