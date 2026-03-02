package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 9: PSP IP whitelist entry.
 */
@Entity
@Table(name = "ip_whitelist")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IpWhitelist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "psp_id", nullable = false)
    private String pspId;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "cidr_range")
    private String cidrRange;

    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
