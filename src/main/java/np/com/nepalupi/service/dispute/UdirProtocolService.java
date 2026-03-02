package np.com.nepalupi.service.dispute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Section 11.1: UDIR (UPI Dispute and Issue Resolution) protocol.
 * <p>
 * Standardized dispute message format for inter-switch dispute exchange.
 * Implements the NPCI UDIR protocol adapted for Nepal (NCHL UDIR).
 * <p>
 * Message types:
 * - ReqComplaint / RespComplaint — dispute creation and response
 * - ReqTxnConfirmation / RespTxnConfirmation — transaction status query
 * <p>
 * Auto-resolution for common cases (Section 11.3.5):
 * - Transaction actually completed → notify user
 * - Debit happened but credit failed → auto-reverse
 * - Transaction PENDING → wait for timeout/settlement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UdirProtocolService {

    private final TransactionRepository transactionRepository;
    private final DisputeService disputeService;

    /**
     * Create a UDIR-compliant dispute message (ReqComplaint).
     */
    public Map<String, Object> createUdirComplaint(String txnId, String complainantVpa,
                                                     String disputeType, String description) {
        // Build ReqComplaint message per UDIR spec
        Map<String, Object> reqComplaint = new LinkedHashMap<>();
        reqComplaint.put("msgType", "ReqComplaint");
        reqComplaint.put("txnId", "UDIR" + UUID.randomUUID().toString().substring(0, 12));
        reqComplaint.put("orgTxnId", txnId);
        reqComplaint.put("complainantAddr", complainantVpa);
        reqComplaint.put("disputeType", disputeType);
        reqComplaint.put("description", description);
        reqComplaint.put("timestamp", Instant.now().toString());
        reqComplaint.put("switchRef", "NCHL-" + System.currentTimeMillis());

        // Perform auto-resolution attempt
        Map<String, Object> autoResolution = attemptAutoResolution(txnId, disputeType);
        reqComplaint.put("autoResolution", autoResolution);

        log.info("UDIR ReqComplaint created: orgTxnId={}, type={}, autoResolved={}",
                txnId, disputeType, autoResolution.get("resolved"));

        return reqComplaint;
    }

    /**
     * Process UDIR response (RespComplaint) from other switch/bank.
     */
    public Map<String, Object> processUdirResponse(Map<String, Object> respComplaint) {
        String orgTxnId = (String) respComplaint.get("orgTxnId");
        String responseCode = (String) respComplaint.get("responseCode");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("msgType", "RespComplaint_ACK");
        result.put("orgTxnId", orgTxnId);
        result.put("timestamp", Instant.now().toString());

        switch (responseCode) {
            case "00" -> {
                // Transaction found, status confirmed
                result.put("status", "CONFIRMED");
                result.put("action", "NOTIFY_COMPLAINANT");
            }
            case "RR" -> {
                // Reversal required
                result.put("status", "REVERSAL_INITIATED");
                result.put("action", "AUTO_REVERSAL");
            }
            case "UR" -> {
                // Under review by bank
                result.put("status", "UNDER_REVIEW");
                result.put("action", "WAIT");
                result.put("expectedResolutionDays", 5);
            }
            default -> {
                result.put("status", "ESCALATED");
                result.put("action", "MANUAL_REVIEW");
            }
        }

        log.info("UDIR RespComplaint processed: orgTxnId={}, responseCode={}, action={}",
                orgTxnId, responseCode, result.get("action"));

        return result;
    }

    /**
     * Build a transaction status query (ReqTxnConfirmation).
     */
    public Map<String, Object> queryTransactionStatus(String txnId) {
        Map<String, Object> reqTxnConfirmation = new LinkedHashMap<>();
        reqTxnConfirmation.put("msgType", "ReqTxnConfirmation");
        reqTxnConfirmation.put("orgTxnId", txnId);
        reqTxnConfirmation.put("timestamp", Instant.now().toString());

        // Look up transaction
        Optional<Transaction> txnOpt = transactionRepository.findByUpiTxnId(txnId);
        if (txnOpt.isEmpty()) {
            reqTxnConfirmation.put("responseCode", "U54"); // Transaction not found
            reqTxnConfirmation.put("status", "NOT_FOUND");
            return reqTxnConfirmation;
        }

        Transaction txn = txnOpt.get();
        reqTxnConfirmation.put("responseCode", "00");
        reqTxnConfirmation.put("status", txn.getStatus().name());
        reqTxnConfirmation.put("payerVpa", txn.getPayerVpa());
        reqTxnConfirmation.put("payeeVpa", txn.getPayeeVpa());
        reqTxnConfirmation.put("amount", txn.getAmount());
        reqTxnConfirmation.put("rrn", txn.getRrn());

        return reqTxnConfirmation;
    }

    /**
     * UDIR escalation levels (Section 11.3.7).
     */
    public Map<String, Object> getEscalationPath(String disputeType) {
        List<Map<String, String>> levels = List.of(
                Map.of("level", "1", "handler", "PSP Customer Support", "sla", "24 hours"),
                Map.of("level", "2", "handler", "Bank Nodal Officer", "sla", "3 business days"),
                Map.of("level", "3", "handler", "NCHL Dispute Cell", "sla", "5 business days"),
                Map.of("level", "4", "handler", "NRB Banking Ombudsman", "sla", "30 business days")
        );

        return Map.of(
                "disputeType", disputeType,
                "escalationLevels", levels,
                "autoResolutionRate", "80%",
                "note", "Most disputes are auto-resolved at Level 1"
        );
    }

    /**
     * Attempt auto-resolution for common dispute types (Section 11.3.5).
     */
    private Map<String, Object> attemptAutoResolution(String txnId, String disputeType) {
        Map<String, Object> result = new LinkedHashMap<>();

        Optional<Transaction> txnOpt = transactionRepository.findByUpiTxnId(txnId);
        if (txnOpt.isEmpty()) {
            result.put("resolved", false);
            result.put("reason", "Original transaction not found");
            return result;
        }

        Transaction txn = txnOpt.get();

        switch (disputeType) {
            case "CREDIT_NOT_RECEIVED" -> {
                if (txn.getStatus() == TransactionStatus.COMPLETED) {
                    result.put("resolved", true);
                    result.put("action", "Transaction actually completed. Credit was posted.");
                    result.put("recommendation", "NOTIFY_USER");
                } else if (txn.getStatus() == TransactionStatus.DEBITED) {
                    result.put("resolved", true);
                    result.put("action", "Debit successful but credit pending — initiating auto-reversal");
                    result.put("recommendation", "AUTO_REVERSE");
                } else {
                    result.put("resolved", false);
                    result.put("reason", "Transaction in status: " + txn.getStatus());
                }
            }
            case "AMOUNT_DEBITED_TXN_FAILED" -> {
                if (txn.getStatus() == TransactionStatus.DEBIT_FAILED ||
                        txn.getStatus() == TransactionStatus.CREDIT_FAILED) {
                    result.put("resolved", true);
                    result.put("action", "Transaction failed — auto-reversal of debit");
                    result.put("recommendation", "AUTO_REVERSE");
                } else if (txn.getStatus() == TransactionStatus.COMPLETED) {
                    result.put("resolved", true);
                    result.put("action", "Transaction actually completed successfully");
                    result.put("recommendation", "NOTIFY_USER");
                } else {
                    result.put("resolved", false);
                    result.put("reason", "Needs manual investigation");
                }
            }
            case "DUPLICATE_TRANSACTION" -> {
                result.put("resolved", false);
                result.put("reason", "Duplicate check requires manual review of transaction history");
                result.put("recommendation", "ESCALATE_L2");
            }
            default -> {
                result.put("resolved", false);
                result.put("reason", "Dispute type requires manual investigation: " + disputeType);
            }
        }

        return result;
    }
}
