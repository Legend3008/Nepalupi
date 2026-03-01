package np.com.nepalupi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DisputeRequest {

    @NotNull(message = "Transaction ID is required")
    private UUID transactionId;

    @NotBlank(message = "Raised-by VPA is required")
    private String raisedByVpa;

    @NotBlank(message = "Reason is required")
    private String reason;

    /** Optional: DEBIT_WITHOUT_CREDIT / FAILED_BUT_DEBITED / UNAUTHORIZED_TRANSACTION / DUPLICATE_CHARGE / OTHER */
    private String disputeType;
}
