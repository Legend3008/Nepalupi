package np.com.nepalupi.domain.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BankResponse {

    private boolean success;
    private String errorCode;
    private String errorMessage;
    private String bankReferenceNumber;

    public static BankResponse ok(String bankRef) {
        return BankResponse.builder()
                .success(true)
                .bankReferenceNumber(bankRef)
                .build();
    }

    public static BankResponse error(String code, String message) {
        return BankResponse.builder()
                .success(false)
                .errorCode(code)
                .errorMessage(message)
                .build();
    }
}
