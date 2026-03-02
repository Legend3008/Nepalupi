package np.com.nepalupi.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotBlank(message = "Payer VPA is required")
    private String payerVpa;

    @NotBlank(message = "Payee VPA is required")
    private String payeeVpa;

    @NotNull(message = "Amount is required")
    @Min(value = 100, message = "Minimum amount is 1 NPR (100 paisa)")
    private Long amount;  // in PAISA

    private String note;

    private String deviceFingerprint;

    private String ipAddress;

    /** User's MPIN — encrypted with bank's public key before transmission */
    private String pin;

    /** Transaction category for NRB category-based limits (P2P, P2M, BILL_PAYMENT, etc.) */
    private String category;

    // Set by the controller from request headers
    private String pspId;
    private String idempotencyKey;
}
