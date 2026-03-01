package np.com.nepalupi.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PspOnboardingRequest {

    @NotBlank(message = "PSP name is required")
    @Size(max = 200)
    private String name;

    @NotBlank(message = "NRB license number is required")
    @Size(max = 50)
    private String nrbLicenseNumber;

    @NotBlank(message = "Technical contact email is required")
    @Email
    private String technicalContactEmail;

    @Size(max = 20)
    private String technicalContactPhone;

    @Size(max = 500)
    private String webhookUrl;

    /** Requested tier: 1, 2, or 3 */
    private Integer requestedTier;
}
