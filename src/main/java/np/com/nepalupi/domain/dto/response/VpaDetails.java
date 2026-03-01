package np.com.nepalupi.domain.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VpaDetails {

    private String vpaAddress;
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
    private java.util.UUID userId;
    private java.util.UUID bankAccountId;
    private boolean active;
}
