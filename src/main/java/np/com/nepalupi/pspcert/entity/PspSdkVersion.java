package np.com.nepalupi.pspcert.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.pspcert.enums.AppPlatform;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "psp_sdk_version")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PspSdkVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "psp_id", nullable = false)
    private String pspId;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_platform", nullable = false)
    private AppPlatform appPlatform;

    @Column(name = "current_sdk_version", nullable = false)
    private String currentSdkVersion;

    @Column(name = "latest_available_version", nullable = false)
    private String latestAvailableVersion;

    @Builder.Default
    @Column(name = "is_current")
    private Boolean isCurrent = false;

    @Builder.Default
    @Column(name = "upgrade_required")
    private Boolean upgradeRequired = false;

    @Column(name = "upgrade_deadline")
    private Instant upgradeDeadline;

    @Builder.Default
    @Column(name = "upgrade_notice_sent")
    private Boolean upgradeNoticeSent = false;

    @Column(name = "upgrade_notice_sent_at")
    private Instant upgradeNoticeSentAt;

    @Builder.Default
    @Column(name = "transactions_restricted")
    private Boolean transactionsRestricted = false;

    @Column(name = "restricted_at")
    private Instant restrictedAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
