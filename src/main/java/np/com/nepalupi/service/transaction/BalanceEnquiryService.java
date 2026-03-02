package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.response.BankResponse;
import np.com.nepalupi.domain.dto.response.VpaDetails;
import np.com.nepalupi.service.bank.BankConnector;
import np.com.nepalupi.service.vpa.VpaResolutionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Balance Enquiry Service — allows users to check their bank account balance via VPA.
 * <p>
 * Flow:
 * 1. Resolve VPA to bank account
 * 2. Send balance enquiry to bank via NCHL
 * 3. Return masked balance response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceEnquiryService {

    private final VpaResolutionService vpaService;
    private final BankConnector bankConnector;

    /**
     * Check balance for a VPA.
     *
     * @param vpa the VPA address (e.g., "ritesh@nchl")
     * @return balance enquiry result
     */
    public Map<String, Object> checkBalance(String vpa) {
        log.info("Balance enquiry for VPA: {}", vpa);

        VpaDetails vpaDetails = vpaService.resolve(vpa);

        BankResponse response = bankConnector.checkBalance(
                vpaDetails.getBankCode(),
                vpaDetails.getAccountNumber());

        if (response.isSuccess()) {
            return Map.of(
                    "vpa", vpa,
                    "bankCode", vpaDetails.getBankCode(),
                    "accountHolder", vpaDetails.getAccountHolderName(),
                    "status", "SUCCESS",
                    "bankRef", response.getBankReferenceNumber(),
                    "enquiredAt", Instant.now().toString()
            );
        } else {
            return Map.of(
                    "vpa", vpa,
                    "status", "FAILED",
                    "errorCode", response.getErrorCode() != null ? response.getErrorCode() : "UNKNOWN",
                    "errorMessage", response.getErrorMessage() != null ? response.getErrorMessage() : "Balance check failed",
                    "enquiredAt", Instant.now().toString()
            );
        }
    }
}
