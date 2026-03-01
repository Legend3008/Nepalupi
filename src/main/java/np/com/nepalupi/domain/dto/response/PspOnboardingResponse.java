package np.com.nepalupi.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PspOnboardingResponse {

    private UUID id;
    private String pspId;
    private String name;
    private String onboardingStage;
    private Integer tier;
    private String nrbLicenseNumber;
    private LocalDate nrbLicenseExpiry;
    private Long perTxnLimitPaisa;
    private Long dailyLimitPaisa;
    private LocalDate pilotStartDate;
    private LocalDate productionDate;
    private String technicalContactEmail;
    private String technicalContactPhone;
    private String webhookUrl;
    private Boolean isActive;
    private String suspensionReason;
    private Instant suspendedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Certification summary
    private Integer mandatoryTestsPassed;
    private Integer mandatoryTestsTotal;
    private Integer advisoryTestsPassed;
    private Integer advisoryTestsTotal;

    // Onboarding history
    private List<StageTransition> stageHistory;

    @Data
    @Builder
    public static class StageTransition {
        private String fromStage;
        private String toStage;
        private String performedBy;
        private String notes;
        private Instant timestamp;
    }
}
