package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tracks the TCP connection state with NCHL's switch (primary and DR).
 * <p>
 * Two rows: one for PRIMARY, one for DR.
 * Updated in real-time as connections are established, signed on,
 * heartbeats sent, and failures occur.
 */
@Entity
@Table(name = "nchl_connection_state")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NchlConnectionState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PRIMARY or DR */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String channel = "PRIMARY";

    /** DISCONNECTED / CONNECTED / SIGNING_ON / SIGNED_ON */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "DISCONNECTED";

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @Column(name = "last_sign_on")
    private Instant lastSignOn;

    @Column(name = "sign_on_count")
    @Builder.Default
    private Integer signOnCount = 0;

    @Column(name = "heartbeat_count")
    @Builder.Default
    private Integer heartbeatCount = 0;

    @Column(name = "fail_count")
    @Builder.Default
    private Integer failCount = 0;

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
