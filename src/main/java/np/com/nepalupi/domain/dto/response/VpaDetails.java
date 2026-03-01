package np.com.nepalupi.domain.dto.response;

import lombok.*;

import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VpaDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vpaAddress;
    private String bankCode;
    private String accountNumber;
    private String accountHolderName;
    private java.util.UUID userId;
    private java.util.UUID bankAccountId;
    private boolean active;
}
