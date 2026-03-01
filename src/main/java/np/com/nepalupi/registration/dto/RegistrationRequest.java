package np.com.nepalupi.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(98|97|96)[0-9]{8}$", message = "Invalid Nepal mobile number")
    private String mobileNumber;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    private String imei;
    private String simSerial;
}
