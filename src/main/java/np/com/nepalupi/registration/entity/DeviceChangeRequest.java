package np.com.nepalupi.registration.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.registration.enums.DeviceChangeStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_change_request")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "old_device_id")
    private String oldDeviceId;

    @Column(name = "new_device_id", nullable = false)
    private String newDeviceId;

    @Column(name = "new_sim_serial")
    private String newSimSerial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceChangeStatus status = DeviceChangeStatus.PENDING_VERIFICATION;

    @Column(name = "mpin_verified")
    private Boolean mpinVerified = false;

    @Column(name = "cooling_ends_at")
    private Instant coolingEndsAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
