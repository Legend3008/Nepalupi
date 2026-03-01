package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "settlement_reports")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SettlementReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "settlement_date", unique = true, nullable = false)
    private LocalDate settlementDate;

    @Column(name = "total_transactions", nullable = false)
    private Integer totalTransactions;

    @Column(name = "total_volume_paisa", nullable = false)
    private Long totalVolumePaisa;

    @Column(name = "net_positions", columnDefinition = "jsonb", nullable = false)
    private String netPositions;

    @Column(nullable = false)
    @Builder.Default
    private String status = "GENERATED";

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
