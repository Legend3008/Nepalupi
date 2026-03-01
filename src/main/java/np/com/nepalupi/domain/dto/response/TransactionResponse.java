package np.com.nepalupi.domain.dto.response;

import lombok.*;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransactionResponse {

    private String upiTxnId;
    private String rrn;
    private String payerVpa;
    private String payeeVpa;
    private Long amount;
    private String currency;
    private TransactionStatus status;
    private String failureCode;
    private String failureReason;
    private Instant initiatedAt;
    private Instant completedAt;
    private boolean success;

    public static TransactionResponse from(Transaction txn) {
        return TransactionResponse.builder()
                .upiTxnId(txn.getUpiTxnId())
                .rrn(txn.getRrn())
                .payerVpa(txn.getPayerVpa())
                .payeeVpa(txn.getPayeeVpa())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .status(txn.getStatus())
                .failureCode(txn.getFailureCode())
                .failureReason(txn.getFailureReason())
                .initiatedAt(txn.getInitiatedAt())
                .completedAt(txn.getCompletedAt())
                .success(txn.getStatus() == TransactionStatus.COMPLETED)
                .build();
    }

    public static TransactionResponse success(Transaction txn) {
        TransactionResponse resp = from(txn);
        resp.setSuccess(true);
        return resp;
    }

    public static TransactionResponse failed(Transaction txn) {
        TransactionResponse resp = from(txn);
        resp.setSuccess(false);
        return resp;
    }
}
