package np.com.nepalupi.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LargeMerchantOnboardRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business category is required")
    private String businessCategory;

    @NotBlank(message = "PAN number is required")
    private String panNumber;

    private String registrationDocBase64;

    @NotBlank(message = "Desired VPA is required")
    private String desiredVpa;

    @NotBlank(message = "Webhook URL is required for large merchants")
    private String webhookUrl;

    private String addressLine;
    private String city;
    private String district;

    private UUID userId;
    private UUID bankAccountId;

    private String settlementCycle;     // T0 or T1
}
