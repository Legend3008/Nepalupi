package np.com.nepalupi.pspcert.entity;

import jakarta.persistence.*;
import lombok.*;
import np.com.nepalupi.pspcert.enums.AppCertificationStage;
import np.com.nepalupi.pspcert.enums.AppCertificationStatus;
import np.com.nepalupi.pspcert.enums.AppPlatform;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "psp_app_certification")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PspAppCertification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "psp_id", nullable = false)
    private String pspId;

    @Column(name = "app_name", nullable = false)
    private String appName;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_platform", nullable = false)
    private AppPlatform appPlatform;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "sdk_version")
    private String sdkVersion;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "stage", nullable = false)
    private AppCertificationStage stage = AppCertificationStage.DESIGN_REVIEW;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false)
    private AppCertificationStatus status = AppCertificationStatus.NOT_STARTED;

    // Design review
    @Builder.Default
    @Column(name = "design_submitted")
    private Boolean designSubmitted = false;

    @Column(name = "design_submitted_at")
    private Instant designSubmittedAt;

    @Builder.Default
    @Column(name = "design_approved")
    private Boolean designApproved = false;

    @Column(name = "design_feedback", columnDefinition = "TEXT")
    private String designFeedback;

    // SDK integration
    @Builder.Default
    @Column(name = "sdk_verified")
    private Boolean sdkVerified = false;

    @Builder.Default
    @Column(name = "cert_pinning_verified")
    private Boolean certPinningVerified = false;

    @Builder.Default
    @Column(name = "pin_pad_sdk_verified")
    private Boolean pinPadSdkVerified = false;

    @Builder.Default
    @Column(name = "no_hardcoded_keys")
    private Boolean noHardcodedKeys = false;

    @Builder.Default
    @Column(name = "permissions_minimal")
    private Boolean permissionsMinimal = false;

    @Builder.Default
    @Column(name = "screenshot_prevention_verified")
    private Boolean screenshotPreventionVerified = false;

    // Functional testing
    @Builder.Default
    @Column(name = "functional_test_passed")
    private Boolean functionalTestPassed = false;

    @Column(name = "functional_test_notes", columnDefinition = "TEXT")
    private String functionalTestNotes;

    // Security testing
    @Builder.Default
    @Column(name = "security_test_passed")
    private Boolean securityTestPassed = false;

    @Builder.Default
    @Column(name = "security_findings_count")
    private Integer securityFindingsCount = 0;

    @Builder.Default
    @Column(name = "security_critical_findings")
    private Integer securityCriticalFindings = 0;

    @Column(name = "security_test_report_url")
    private String securityTestReportUrl;

    // Performance testing
    @Builder.Default
    @Column(name = "performance_test_passed")
    private Boolean performanceTestPassed = false;

    @Column(name = "avg_pin_screen_time_ms")
    private Integer avgPinScreenTimeMs;

    @Column(name = "avg_confirmation_time_ms")
    private Integer avgConfirmationTimeMs;

    @Column(name = "avg_history_load_time_ms")
    private Integer avgHistoryLoadTimeMs;

    // Compliance review
    @Builder.Default
    @Column(name = "compliance_passed")
    private Boolean compliancePassed = false;

    @Builder.Default
    @Column(name = "terms_present")
    private Boolean termsPresent = false;

    @Builder.Default
    @Column(name = "privacy_policy_present")
    private Boolean privacyPolicyPresent = false;

    @Builder.Default
    @Column(name = "grievance_officer_displayed")
    private Boolean grievanceOfficerDisplayed = false;

    @Builder.Default
    @Column(name = "dispute_accessible")
    private Boolean disputeAccessible = false;

    // Pilot
    @Column(name = "pilot_start_date")
    private LocalDate pilotStartDate;

    @Column(name = "pilot_end_date")
    private LocalDate pilotEndDate;

    @Builder.Default
    @Column(name = "pilot_user_count")
    private Integer pilotUserCount = 0;

    @Column(name = "pilot_feedback_summary", columnDefinition = "TEXT")
    private String pilotFeedbackSummary;

    // Launch
    @Column(name = "launched_at")
    private Instant launchedAt;

    @Column(name = "reviewer")
    private String reviewer;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
