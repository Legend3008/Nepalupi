package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transaction receipt generator.
 * Creates structured receipt data for completed transactions.
 * In production, integrates with a PDF library (iTextPDF, Apache PDFBox).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionReceiptService {

    private final TransactionRepository transactionRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
            .withZone(ZoneId.of("Asia/Kathmandu"));

    /**
     * Generate a structured receipt for a completed transaction.
     */
    public Map<String, Object> generateReceipt(String upiTxnId) {
        Transaction txn = transactionRepository.findByUpiTxnId(upiTxnId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + upiTxnId));

        if (txn.getStatus() != TransactionStatus.COMPLETED &&
            txn.getStatus() != TransactionStatus.REVERSED) {
            throw new IllegalStateException("Receipt available only for COMPLETED or REVERSED transactions");
        }

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("receiptId", "RCPT-" + upiTxnId);
        receipt.put("upiTxnId", txn.getUpiTxnId());
        receipt.put("rrn", txn.getRrn());
        receipt.put("status", txn.getStatus().name());
        receipt.put("type", txn.getTxnType());

        // Payer details
        Map<String, String> payer = new LinkedHashMap<>();
        payer.put("vpa", txn.getPayerVpa());
        payer.put("bankCode", txn.getPayerBankCode());
        receipt.put("payer", payer);

        // Payee details
        Map<String, String> payee = new LinkedHashMap<>();
        payee.put("vpa", txn.getPayeeVpa());
        payee.put("bankCode", txn.getPayeeBankCode());
        receipt.put("payee", payee);

        // Amount
        receipt.put("amountPaisa", txn.getAmount());
        receipt.put("amountNPR", txn.getAmount() / 100.0);
        receipt.put("currency", "NPR");

        // Timestamps
        receipt.put("initiatedAt", DATE_FORMAT.format(txn.getInitiatedAt()));
        if (txn.getCompletedAt() != null) {
            receipt.put("completedAt", DATE_FORMAT.format(txn.getCompletedAt()));
        }

        // Note
        if (txn.getNote() != null) {
            receipt.put("remark", txn.getNote());
        }

        // Footer
        receipt.put("generatedAt", DATE_FORMAT.format(java.time.Instant.now()));
        receipt.put("disclaimer", "This is a computer-generated receipt from NUPI (Nepal Unified Payment Interface). Regulated by Nepal Rastra Bank.");

        log.info("Receipt generated for transaction: {}", upiTxnId);
        return receipt;
    }

    /**
     * Generate receipt as downloadable text (placeholder for PDF).
     * In production, use iTextPDF or Apache PDFBox to generate proper PDF.
     */
    public String generateReceiptText(String upiTxnId) {
        Map<String, Object> receipt = generateReceipt(upiTxnId);

        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════╗\n");
        sb.append("║       NUPI TRANSACTION RECEIPT       ║\n");
        sb.append("╠══════════════════════════════════════╣\n");
        sb.append(String.format("║ Txn ID: %-28s ║\n", receipt.get("upiTxnId")));
        sb.append(String.format("║ RRN   : %-28s ║\n", receipt.get("rrn")));
        sb.append(String.format("║ Status: %-28s ║\n", receipt.get("status")));
        sb.append("╠══════════════════════════════════════╣\n");

        @SuppressWarnings("unchecked")
        Map<String, String> payerInfo = (Map<String, String>) receipt.get("payer");
        @SuppressWarnings("unchecked")
        Map<String, String> payeeInfo = (Map<String, String>) receipt.get("payee");

        sb.append(String.format("║ From  : %-28s ║\n", payerInfo.get("vpa")));
        sb.append(String.format("║ To    : %-28s ║\n", payeeInfo.get("vpa")));
        sb.append(String.format("║ Amount: NPR %-24.2f ║\n", (Double) receipt.get("amountNPR")));
        sb.append("╠══════════════════════════════════════╣\n");
        sb.append(String.format("║ Date  : %-28s ║\n", receipt.get("completedAt")));
        sb.append("╚══════════════════════════════════════╝\n");
        sb.append("Powered by Nepal Rastra Bank\n");

        return sb.toString();
    }
}
