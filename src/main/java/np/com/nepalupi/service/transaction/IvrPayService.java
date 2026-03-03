package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.IvrPaymentSession;
import np.com.nepalupi.repository.IvrPaymentSessionRepository;
import np.com.nepalupi.service.vpa.MobileVpaLookupService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * IVR/123Pay service for feature phone UPI payments.
 * Allows payments via missed call or IVR (Interactive Voice Response).
 * Users dial a short code, follow DTMF prompts to authorize payments.
 * Enables UPI access for non-smartphone users (per NRB digital inclusion mandate).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IvrPayService {

    private final IvrPaymentSessionRepository ivrRepository;
    private final MobileVpaLookupService mobileVpaLookupService;

    // Maximum amount for IVR payments (lower limit for security)
    private static final long IVR_MAX_AMOUNT_PAISA = 500000L; // Rs 5,000

    /**
     * Create a new IVR payment session when a call comes in.
     */
    public IvrPaymentSession createSession(String callerMobile, String language) {
        IvrPaymentSession session = IvrPaymentSession.builder()
                .callerMobile(callerMobile)
                .amountPaisa(0L) // Set by DTMF input later
                .ivrSessionId("IVR-" + UUID.randomUUID().toString().substring(0, 12))
                .status("INITIATED")
                .language(language != null ? language : "ne")
                .build();

        ivrRepository.save(session);
        log.info("IVR session created: mobile={}, sessionId={}", callerMobile, session.getIvrSessionId());
        return session;
    }

    /**
     * Process DTMF input from the user.
     * Flow: 1=Send money, 2=Check balance, 3=Recent transactions, *=Cancel
     * For Send money: enter payee VPA digits → amount → MPIN
     */
    public IvrPaymentSession processDtmfInput(UUID sessionId, String dtmfInput) {
        IvrPaymentSession session = findById(sessionId);
        session.setDtmfInput(dtmfInput);

        // Parse the DTMF flow
        if ("*".equals(dtmfInput)) {
            session.setStatus("CANCELLED");
            ivrRepository.save(session);
            return session;
        }

        // DTMF format: OPTION#PAYEE_MOBILE#AMOUNT
        String[] parts = dtmfInput.split("#");
        if (parts.length >= 3) {
            String option = parts[0];
            String payeeMobile = parts[1];
            String amountStr = parts[2];

            if ("1".equals(option)) { // Send money
                try {
                    long amount = Long.parseLong(amountStr) * 100; // Convert Rs to paisa
                    if (amount > IVR_MAX_AMOUNT_PAISA) {
                        session.setStatus("AMOUNT_EXCEEDED");
                        ivrRepository.save(session);
                        return session;
                    }
                    session.setAmountPaisa(amount);
                    // Resolve payee mobile to VPA
                    Map<String, Object> resolved = mobileVpaLookupService.resolveByMobile(payeeMobile);
                    String payeeVpa = (String) resolved.get("vpa");
                    session.setCalleeVpa(payeeVpa);
                    session.setStatus("AWAITING_PIN");
                } catch (NumberFormatException e) {
                    session.setStatus("INVALID_INPUT");
                }
            }
        }

        ivrRepository.save(session);
        log.info("DTMF processed: sessionId={}, input={}, status={}", sessionId, dtmfInput, session.getStatus());
        return session;
    }

    /**
     * Authorize the IVR payment after MPIN verification via DTMF.
     */
    public IvrPaymentSession authorize(UUID sessionId) {
        IvrPaymentSession session = findById(sessionId);
        if (!"AWAITING_PIN".equals(session.getStatus())) {
            throw new IllegalStateException("Session not ready for authorization");
        }
        session.setStatus("AUTHORIZED");
        ivrRepository.save(session);

        // In production: delegate to TransactionOrchestrator
        log.info("IVR payment authorized: sessionId={}, payee={}, amount={}",
                sessionId, session.getCalleeVpa(), session.getAmountPaisa());
        return session;
    }

    /**
     * Complete the IVR payment.
     */
    public IvrPaymentSession complete(UUID sessionId, UUID transactionId) {
        IvrPaymentSession session = findById(sessionId);
        session.setStatus("COMPLETED");
        session.setTransactionId(transactionId);
        ivrRepository.save(session);
        return session;
    }

    /**
     * Get the IVR menu script for TTS (Text-to-Speech).
     */
    public Map<String, String> getMenuScript(String language) {
        Map<String, String> script = new LinkedHashMap<>();
        if ("en".equals(language)) {
            script.put("welcome", "Welcome to NUPI 123Pay. Press 1 to send money, 2 to check balance, 3 for recent transactions, star to cancel.");
            script.put("enter_mobile", "Enter the recipient's mobile number followed by hash.");
            script.put("enter_amount", "Enter the amount in rupees followed by hash.");
            script.put("enter_pin", "Enter your 4-digit MPIN followed by hash.");
            script.put("success", "Payment successful. Amount {amount} sent to {payee}.");
            script.put("failure", "Payment failed. Please try again.");
        } else {
            script.put("welcome", "NUPI 123Pay मा स्वागत छ। पैसा पठाउन 1, ब्यालेन्स जाँच्न 2, हालैका लेनदेन 3, रद्द गर्न * थिच्नुहोस्।");
            script.put("enter_mobile", "प्राप्तकर्ताको मोबाइल नम्बर राख्नुहोस् र # थिच्नुहोस्।");
            script.put("enter_amount", "रकम रुपैयाँमा राख्नुहोस् र # थिच्नुहोस्।");
            script.put("enter_pin", "तपाईंको 4 अंकको MPIN राख्नुहोस् र # थिच्नुहोस्।");
            script.put("success", "भुक्तानी सफल भयो। रु {amount} {payee} लाई पठाइयो।");
            script.put("failure", "भुक्तानी असफल भयो। कृपया पुन: प्रयास गर्नुहोस्।");
        }
        return script;
    }

    public List<IvrPaymentSession> getSessionsByMobile(String callerMobile) {
        return ivrRepository.findByCallerMobileOrderByCreatedAtDesc(callerMobile);
    }

    private IvrPaymentSession findById(UUID id) {
        return ivrRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("IVR session not found: " + id));
    }
}
