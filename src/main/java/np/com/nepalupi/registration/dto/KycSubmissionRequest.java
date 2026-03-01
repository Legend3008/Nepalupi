package np.com.nepalupi.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KycSubmissionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "KYC level is required")
    private String kycLevel;   // MINIMUM or FULL

    // Full KYC fields
    private String documentType;       // CITIZENSHIP_CERTIFICATE / PASSPORT / NATIONAL_ID
    private String documentNumber;
    private String documentImageBase64;
    private String selfieImageBase64;
}
