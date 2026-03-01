package np.com.nepalupi.registration.dto;

import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistrationResponse {
    private UUID userId;
    private String mobileNumber;
    private String status;    // DEVICE_BINDING_PENDING / SIM_VERIFIED / REGISTERED
    private String message;
    private String bindingSmsId;
}
