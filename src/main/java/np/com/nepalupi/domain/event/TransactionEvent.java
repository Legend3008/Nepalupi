package np.com.nepalupi.domain.event;

import lombok.*;
import np.com.nepalupi.domain.enums.TransactionStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Published to Kafka whenever a transaction state changes.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TransactionEvent implements Serializable {

    private String txnId;
    private String upiTxnId;
    private String rrn;
    private String payerVpa;
    private String payeeVpa;
    private UUID payerUserId;
    private UUID payeeUserId;
    private Long amount;
    private String currency;
    private TransactionStatus status;
    private String failureCode;
    private Instant occurredAt;

    @Builder.Default
    private String eventType = "TRANSACTION_STATE_CHANGE";
}
