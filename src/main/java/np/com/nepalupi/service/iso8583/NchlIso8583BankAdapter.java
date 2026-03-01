package np.com.nepalupi.service.iso8583;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.response.BankResponse;
import np.com.nepalupi.domain.enums.Iso8583ResponseCode;
import np.com.nepalupi.exception.BankTimeoutException;
import np.com.nepalupi.service.bank.BankAdapter;
import np.com.nepalupi.util.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Real NCHL Bank Adapter using ISO 8583 messages.
 * <p>
 * This replaces the mock adapter in production. All banks are reached
 * through NCHL's switch — same architecture as Indian UPI where all
 * banks connect through NPCI.
 * <p>
 * Flow:
 * <ol>
 *   <li>Build ISO 8583 message with correct fields</li>
 *   <li>Send via NchlChannelManager (TCP to NCHL switch)</li>
 *   <li>Receive 0210 response</li>
 *   <li>Map field 39 response code to our BankResponse</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NchlIso8583BankAdapter implements BankAdapter {

    private final NchlChannelManager channelManager;
    private final Iso8583MessageBuilder messageBuilder;
    private final IdGenerator idGenerator;

    @Override
    public String getBankCode() {
        return "NCHL";
    }

    @Override
    public BankResponse debit(String accountNumber, Long amountPaisa, String txnId) {
        log.info("ISO8583 DEBIT — account: {}****, amount: {} paisa, txn: {}",
                accountNumber.substring(0, Math.min(4, accountNumber.length())), amountPaisa, txnId);

        String rrn = idGenerator.generateRRN();
        String stan = idGenerator.generateStan();

        Iso8583MessageBuilder.Iso8583Message request = messageBuilder.buildDebitRequest(
                accountNumber, amountPaisa, rrn, stan, null /* PIN handled separately */);

        try {
            UUID transactionUuid = parseTransactionId(txnId);
            Iso8583MessageBuilder.Iso8583Message response = channelManager.sendMessage(request, transactionUuid);

            return mapResponse(response);
        } catch (IllegalStateException e) {
            log.error("NCHL channel not ready for debit: {}", e.getMessage());
            throw new BankTimeoutException("NCHL channel unavailable", e);
        } catch (Exception e) {
            log.error("ISO8583 debit failed for txn {}: {}", txnId, e.getMessage());
            throw new BankTimeoutException("Debit communication error", e);
        }
    }

    @Override
    public BankResponse credit(String accountNumber, Long amountPaisa, String txnId) {
        log.info("ISO8583 CREDIT — account: {}****, amount: {} paisa, txn: {}",
                accountNumber.substring(0, Math.min(4, accountNumber.length())), amountPaisa, txnId);

        String rrn = idGenerator.generateRRN();
        String stan = idGenerator.generateStan();

        Iso8583MessageBuilder.Iso8583Message request = messageBuilder.buildCreditRequest(
                accountNumber, amountPaisa, rrn, stan);

        try {
            UUID transactionUuid = parseTransactionId(txnId);
            Iso8583MessageBuilder.Iso8583Message response = channelManager.sendMessage(request, transactionUuid);

            return mapResponse(response);
        } catch (IllegalStateException e) {
            log.error("NCHL channel not ready for credit: {}", e.getMessage());
            throw new BankTimeoutException("NCHL channel unavailable", e);
        } catch (Exception e) {
            log.error("ISO8583 credit failed for txn {}: {}", txnId, e.getMessage());
            throw new BankTimeoutException("Credit communication error", e);
        }
    }

    @Override
    public BankResponse checkBalance(String accountNumber) {
        log.info("ISO8583 balance inquiry not yet implemented via NCHL");
        return BankResponse.error("NOT_IMPLEMENTED", "Balance check via NCHL not available yet");
    }

    @Override
    public BankResponse reversal(String originalTxnId) {
        log.info("ISO8583 REVERSAL — original txn: {}", originalTxnId);

        String stan = idGenerator.generateStan();
        String originalRrn = originalTxnId; // In production, look up original RRN from audit log

        Iso8583MessageBuilder.Iso8583Message request = messageBuilder.buildReversalRequest(
                "0000000000", 0, originalRrn, "000000", stan);

        try {
            UUID transactionUuid = parseTransactionId(originalTxnId);
            Iso8583MessageBuilder.Iso8583Message response = channelManager.sendMessage(request, transactionUuid);

            return mapResponse(response);
        } catch (Exception e) {
            log.error("ISO8583 reversal failed for txn {}: {}", originalTxnId, e.getMessage());
            throw new BankTimeoutException("Reversal communication error", e);
        }
    }

    /**
     * Map ISO 8583 field 39 response code to our BankResponse.
     */
    private BankResponse mapResponse(Iso8583MessageBuilder.Iso8583Message response) {
        String responseCode = response.getField39();
        Iso8583ResponseCode rc = Iso8583ResponseCode.fromCode(responseCode);

        if (rc.isApproved()) {
            return BankResponse.ok(response.getField38());
        }

        log.warn("Bank declined: code={} ({})", rc.getCode(), rc.getDescription());
        return BankResponse.error(rc.getCode(), rc.getDescription());
    }

    private UUID parseTransactionId(String txnId) {
        try {
            return UUID.fromString(txnId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
