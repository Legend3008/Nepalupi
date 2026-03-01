package np.com.nepalupi.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MpinSetupRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Bank code is required")
    private String bankCode;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Card last 6 digits required for MPIN setup")
    @Size(min = 6, max = 6, message = "Must be last 6 digits of debit card")
    private String cardLast6Digits;

    @NotBlank(message = "Card expiry MM/YY required")
    private String cardExpiry;

    /**
     * Encrypted MPIN (4-6 digits, encrypted by PSP app with device key).
     * UPI switch never sees plaintext MPIN.
     */
    @NotBlank(message = "Encrypted MPIN is required")
    private String encryptedMpin;
}
