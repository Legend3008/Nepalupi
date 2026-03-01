package np.com.nepalupi.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceChangeRequestDto {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "New device ID is required")
    private String newDeviceId;

    private String newSimSerial;

    /**
     * Encrypted MPIN for identity verification during device change.
     */
    @NotBlank(message = "Encrypted MPIN is required for device change")
    private String encryptedMpin;
}
