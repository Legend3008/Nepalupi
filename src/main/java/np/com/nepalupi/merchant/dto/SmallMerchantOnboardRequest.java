package np.com.nepalupi.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SmallMerchantOnboardRequest {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business category is required")
    private String businessCategory;   // MerchantCategory enum name

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    private UUID userId;               // if already a UPI user

    @NotBlank(message = "Desired VPA is required")
    private String desiredVpa;         // e.g., "ramteastall@nepalupi"

    private String addressLine;
    private String city;
    private String district;

    private UUID bankAccountId;        // settlement account
}
