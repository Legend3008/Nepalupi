package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.NfcPaymentSession;
import np.com.nepalupi.repository.NfcPaymentSessionRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NFC Tap & Pay service for contactless UPI payments.
 * Supports HCE (Host Card Emulation) and NFC tag reading.
 * Compliant with EMVCo contactless specifications adapted for Nepal UPI.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NfcPayService {

    private final NfcPaymentSessionRepository nfcRepository;

    // Maximum amount for tap-and-go (without PIN)
    private static final long TAP_GO_LIMIT_PAISA = 500000L; // Rs 5,000

    /**
     * Create an NFC payment session when device detects NFC tag/terminal.
     */
    public NfcPaymentSession createSession(UUID userId, String merchantVpa, Long amountPaisa,
                                            String nfcTagData, String cardEmulationMode, String terminalId) {
        NfcPaymentSession session = NfcPaymentSession.builder()
                .userId(userId)
                .merchantVpa(merchantVpa)
                .amountPaisa(amountPaisa)
                .nfcTagData(nfcTagData)
                .cardEmulationMode(cardEmulationMode != null ? cardEmulationMode : "HCE")
                .terminalId(terminalId)
                .status("INITIATED")
                .build();

        nfcRepository.save(session);
        log.info("NFC session created: userId={}, merchant={}, amount={}", userId, merchantVpa, amountPaisa);
        return session;
    }

    /**
     * Check if the NFC payment can proceed as tap-and-go (no PIN required).
     */
    public boolean isTapAndGoEligible(Long amountPaisa) {
        return amountPaisa <= TAP_GO_LIMIT_PAISA;
    }

    /**
     * Authorize the NFC payment session.
     * For amounts ≤ TAP_GO_LIMIT, no PIN needed.
     * For amounts > TAP_GO_LIMIT, MPIN verification required first.
     */
    public NfcPaymentSession authorize(UUID sessionId, boolean pinVerified) {
        NfcPaymentSession session = findById(sessionId);

        if (session.getAmountPaisa() > TAP_GO_LIMIT_PAISA && !pinVerified) {
            session.setStatus("AWAITING_PIN");
            nfcRepository.save(session);
            return session;
        }

        session.setStatus("AUTHORIZED");
        nfcRepository.save(session);

        // In production: delegate to TransactionOrchestrator for actual settlement
        log.info("NFC payment authorized: sessionId={}, amount={}", sessionId, session.getAmountPaisa());
        return session;
    }

    /**
     * Complete the NFC payment after settlement.
     */
    public NfcPaymentSession complete(UUID sessionId, UUID transactionId) {
        NfcPaymentSession session = findById(sessionId);
        session.setStatus("COMPLETED");
        session.setTransactionId(transactionId);
        nfcRepository.save(session);
        log.info("NFC payment completed: sessionId={}, txnId={}", sessionId, transactionId);
        return session;
    }

    /**
     * Cancel/fail the NFC session.
     */
    public NfcPaymentSession cancel(UUID sessionId, String reason) {
        NfcPaymentSession session = findById(sessionId);
        session.setStatus("CANCELLED");
        nfcRepository.save(session);
        log.info("NFC session cancelled: sessionId={}, reason={}", sessionId, reason);
        return session;
    }

    /**
     * Parse NFC tag data to extract merchant VPA and amount.
     * Supports NDEF (NFC Data Exchange Format) records.
     */
    public Map<String, String> parseNfcTag(String nfcTagData) {
        Map<String, String> parsed = new HashMap<>();
        // NDEF record format: key=value pairs separated by |
        if (nfcTagData != null && !nfcTagData.isBlank()) {
            for (String pair : nfcTagData.split("\\|")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    parsed.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        return parsed;
    }

    public List<NfcPaymentSession> getUserSessions(UUID userId) {
        return nfcRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private NfcPaymentSession findById(UUID id) {
        return nfcRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("NFC session not found: " + id));
    }
}
