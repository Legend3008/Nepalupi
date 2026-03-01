package np.com.nepalupi.certification.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.certification.enums.CertificationStage;
import np.com.nepalupi.certification.enums.CertificationStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_certification")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BankCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "stage", nullable = false)
    private CertificationStage stage = CertificationStage.TECHNICAL_ONBOARDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private CertificationStatus status = CertificationStatus.NOT_STARTED;

    @Column(name = "technical_contact_name")
    private String technicalContactName;

    @Column(name = "technical_contact_email")
    private String technicalContactEmail;

    @Column(name = "technical_contact_phone")
    private String technicalContactPhone;

    @Builder.Default
    @Column(name = "agreement_signed")
    private Boolean agreementSigned = false;

    @Column(name = "agreement_signed_at")
    private Instant agreementSignedAt;

    @Builder.Default
    @Column(name = "sandbox_credentials_issued")
    private Boolean sandboxCredentialsIssued = false;

    @Column(name = "sandbox_credentials_issued_at")
    private Instant sandboxCredentialsIssuedAt;

    @Builder.Default
    @Column(name = "documentation_delivered")
    private Boolean documentationDelivered = false;

    @Column(name = "documentation_delivered_at")
    private Instant documentationDeliveredAt;

    @Builder.Default
    @Column(name = "self_cert_submitted")
    private Boolean selfCertSubmitted = false;

    @Column(name = "self_cert_submitted_at")
    private Instant selfCertSubmittedAt;

    @Builder.Default
    @Column(name = "self_cert_passed")
    private Boolean selfCertPassed = false;

    @Column(name = "formal_cert_scheduled_at")
    private Instant formalCertScheduledAt;

    @Column(name = "formal_cert_completed_at")
    private Instant formalCertCompletedAt;

    @Builder.Default
    @Column(name = "formal_cert_passed")
    private Boolean formalCertPassed = false;

    @Column(name = "mandatory_pass_rate")
    private BigDecimal mandatoryPassRate;

    @Column(name = "advisory_pass_rate")
    private BigDecimal advisoryPassRate;

    @Column(name = "parallel_start_date")
    private LocalDate parallelStartDate;

    @Column(name = "parallel_end_date")
    private LocalDate parallelEndDate;

    @Builder.Default
    @Column(name = "parallel_daily_limit")
    private Integer parallelDailyLimit = 100;

    @Builder.Default
    @Column(name = "parallel_anomalies_found")
    private Integer parallelAnomaliesFound = 0;

    @Column(name = "production_go_live_date")
    private LocalDate productionGoLiveDate;

    @Column(name = "recertification_due_date")
    private LocalDate recertificationDueDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
