package np.com.nepalupi.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.certification.enums.ExecutionPhase;
import np.com.nepalupi.certification.enums.TestResult;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cert_test_result")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CertTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "certification_id", nullable = false)
    private UUID certificationId;

    @Column(name = "test_case_id", nullable = false)
    private UUID testCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_phase", nullable = false)
    private ExecutionPhase executionPhase;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private TestResult result;

    @Column(name = "request_sent", columnDefinition = "TEXT")
    private String requestSent;

    @Column(name = "response_received", columnDefinition = "TEXT")
    private String responseReceived;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "executed_by")
    private String executedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
