package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.service.transaction.ReversalService;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Merchant refund service for processing full or partial refunds.
 * Integrates with the reversal service for settlement adjustment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantRefundService {

    private final TransactionRepository transactionRepository;
    private final ReversalService reversalService;

    /**
     * Initiate a full refund for a merchant transaction.
     */
    public Map<String, Object> initiateFullRefund(UUID transactionId, String merchantVpa, String reason) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Verify the merchant is the payee
        if (!merchantVpa.equals(txn.getPayeeVpa())) {
            throw new IllegalArgumentException("Transaction does not belong to merchant: " + merchantVpa);
        }

        // Delegate to reversal service
        try {
            reversalService.initiateReversal(txn);

            Map<String, Object> result = new HashMap<>();
            result.put("transactionId", transactionId);
            result.put("refundAmount", txn.getAmount());
            result.put("status", "REFUND_INITIATED");
            result.put("reason", reason);
            result.put("type", "FULL");

            log.info("Full refund initiated: txnId={}, merchant={}, amount={}", transactionId, merchantVpa, txn.getAmount());
            return result;
        } catch (Exception e) {
            log.error("Refund failed: txnId={}, error={}", transactionId, e.getMessage());
            throw new RuntimeException("Refund initiation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Initiate a partial refund.
     */
    public Map<String, Object> initiatePartialRefund(UUID transactionId, String merchantVpa,
                                                      Long refundAmountPaisa, String reason) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (!merchantVpa.equals(txn.getPayeeVpa())) {
            throw new IllegalArgumentException("Transaction does not belong to merchant: " + merchantVpa);
        }

        if (refundAmountPaisa > txn.getAmount()) {
            throw new IllegalArgumentException("Refund amount exceeds transaction amount");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", transactionId);
        result.put("originalAmount", txn.getAmount());
        result.put("refundAmount", refundAmountPaisa);
        result.put("status", "PARTIAL_REFUND_INITIATED");
        result.put("reason", reason);
        result.put("type", "PARTIAL");

        log.info("Partial refund initiated: txnId={}, merchant={}, refund={}/{}",
                transactionId, merchantVpa, refundAmountPaisa, txn.getAmount());
        return result;
    }

    /**
     * Get refund eligibility for a transaction.
     */
    public Map<String, Object> checkRefundEligibility(UUID transactionId) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        Map<String, Object> eligibility = new HashMap<>();
        eligibility.put("transactionId", transactionId);
        eligibility.put("amount", txn.getAmount());
        eligibility.put("status", txn.getStatus());
        eligibility.put("isEligible", "COMPLETED".equals(txn.getStatus()));
        eligibility.put("isPartialRefundSupported", true);

        return eligibility;
    }
}
