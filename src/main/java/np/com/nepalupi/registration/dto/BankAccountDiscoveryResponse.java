package np.com.nepalupi.registration.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccountDiscoveryResponse {

    private String mobileNumber;
    private List<DiscoveredAccount> accounts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DiscoveredAccount {
        private String bankCode;
        private String bankName;
        private String maskedAccountNumber;
        private String accountType;
        private boolean ifscAvailable;
    }
}
