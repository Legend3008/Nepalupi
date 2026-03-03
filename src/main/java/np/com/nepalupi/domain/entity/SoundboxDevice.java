package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "soundbox_devices")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SoundboxDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "device_serial", unique = true, nullable = false, length = 100)
    private String deviceSerial;

    @Column(name = "device_model", length = 50)
    private String deviceModel;

    @Column(name = "firmware_version", length = 20)
    private String firmwareVersion;

    @Column(name = "sim_number", length = 20)
    private String simNumber;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(length = 10)
    @Builder.Default
    private String language = "ne";

    @Column(name = "volume_level")
    @Builder.Default
    private Integer volumeLevel = 80;

    @Column(name = "registered_at")
    @Builder.Default
    private Instant registeredAt = Instant.now();
}
