package np.com.nepalupi.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.certification.enums.TestCaseCategory;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cert_test_case")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CertTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_code", nullable = false, unique = true)
    private String testCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private TestCaseCategory category;

    @Builder.Default
    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_behavior", nullable = false, columnDefinition = "TEXT")
    private String expectedBehavior;

    @Column(name = "iso_message_template", columnDefinition = "TEXT")
    private String isoMessageTemplate;

    @Column(name = "expected_response_code")
    private String expectedResponseCode;

    @Builder.Default
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 30;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
