package np.com.nepalupi.registration.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.registration.enums.DeviceBindingStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_binding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    private String imei;

    @Column(name = "sim_serial")
    private String simSerial;

    @Column(name = "binding_sms_id")
    private String bindingSmsId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceBindingStatus status = DeviceBindingStatus.PENDING;

    @Column(name = "bound_at")
    private Instant boundAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
