package np.com.nepalupi.merchant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DynamicQrRequest {

    @NotNull(message = "Merchant ID is required")
    private UUID merchantId;

    @Positive(message = "Amount must be positive")
    private Long amountPaisa;

    private String description;

    /**
     * Unique reference from the merchant's system — enables reconciliation.
     */
    private String merchantTxnRef;

    /**
     * Expiry in seconds (default 15 minutes).
     */
    private Integer expireSeconds;
}
