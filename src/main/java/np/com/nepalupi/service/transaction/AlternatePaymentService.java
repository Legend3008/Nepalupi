package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.request.PaymentRequest;
import np.com.nepalupi.domain.dto.response.TransactionResponse;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.repository.BankAccountRepository;
import np.com.nepalupi.repository.VpaRepository;
import np.com.nepalupi.service.vpa.MobileVpaLookupService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Extended payment service supporting send-via-mobile and send-via-account+bankcode.
 * Resolves alternative identifiers to VPA, then delegates to TransactionOrchestrator.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlternatePaymentService {

    private final TransactionOrchestrator orchestrator;
    private final MobileVpaLookupService mobileVpaLookupService;
    private final BankAccountRepository bankAccountRepository;
    private final VpaRepository vpaRepository;

    /**
     * Send money via mobile number.
     * Resolves mobile → VPA, then initiates standard UPI payment.
     */
    public TransactionResponse sendViaMobile(String payerVpa, String payeeMobile, Long amount,
                                              String note, String idempotencyKey, String ipAddress) {
        Map<String, Object> resolved = mobileVpaLookupService.resolveByMobile(payeeMobile);
        String payeeVpa = (String) resolved.get("vpa");

        log.info("Send via mobile: {} → {} (resolved VPA: {})", payerVpa, payeeMobile, payeeVpa);

        PaymentRequest request = PaymentRequest.builder()
                .payerVpa(payerVpa)
                .payeeVpa(payeeVpa)
                .amount(amount)
                .note(note != null ? note : "Payment via mobile")
                .idempotencyKey(idempotencyKey)
                .ipAddress(ipAddress)
                .build();

        return orchestrator.initiatePayment(request);
    }

    /**
     * Send money via bank account + bank code (Nepal equivalent of IFSC).
     * Looks up the VPA linked to that bank account, then initiates payment.
     */
    public TransactionResponse sendViaAccount(String payerVpa, String payeeBankCode,
                                               String payeeAccountNumber, Long amount,
                                               String note, String idempotencyKey, String ipAddress) {

        // Find the bank account
        BankAccount account = bankAccountRepository.findByBankCodeAndAccountNumber(payeeBankCode, payeeAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found: " + payeeBankCode + " / " + payeeAccountNumber));

        // Find VPA linked to this account
        List<Vpa> vpas = vpaRepository.findByUserIdAndIsActiveTrue(account.getUserId());
        String payeeVpa = vpas.stream()
                .filter(Vpa::getIsPrimary)
                .findFirst()
                .orElse(vpas.isEmpty() ? null : vpas.get(0))
                .getVpaAddress();

        if (payeeVpa == null) {
            throw new IllegalStateException("No VPA linked to account: " + payeeAccountNumber);
        }

        log.info("Send via account: {} → {}/{} (resolved VPA: {})",
                payerVpa, payeeBankCode, maskAccount(payeeAccountNumber), payeeVpa);

        PaymentRequest request = PaymentRequest.builder()
                .payerVpa(payerVpa)
                .payeeVpa(payeeVpa)
                .amount(amount)
                .note(note != null ? note : "Payment via account")
                .idempotencyKey(idempotencyKey)
                .ipAddress(ipAddress)
                .build();

        return orchestrator.initiatePayment(request);
    }

    private String maskAccount(String account) {
        if (account == null || account.length() < 4) return "****";
        return "****" + account.substring(account.length() - 4);
    }
}
