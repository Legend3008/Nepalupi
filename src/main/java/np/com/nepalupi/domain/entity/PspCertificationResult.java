package np.com.nepalupi.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * PSP certification test result — each test case a PSP runs in sandbox.
 * <p>
 * Must pass 100% mandatory + 80% advisory before production credentials.
 */
@Entity
@Table(name = "psp_certification_result")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PspCertificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "psp_id", nullable = false, length = 50)
    private String pspId;

    /** VPA_RESOLUTION / PAYMENT_INITIATION / PIN_FLOW / STATUS_QUERY / WEBHOOK / ERROR_HANDLING */
    @Column(name = "test_suite", nullable = false, length = 50)
    private String testSuite;

    @Column(name = "test_case", nullable = false, length = 200)
    private String testCase;

    @Column(nullable = false)
    @Builder.Default
    private Boolean mandatory = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean passed = false;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "executed_at")
    @Builder.Default
    private Instant executedAt = Instant.now();
}
