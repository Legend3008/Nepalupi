package np.com.nepalupi.service.bank;

import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.response.BankResponse;
import np.com.nepalupi.util.IdGenerator;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of the NCHL (Nepal Clearing House Limited) bank adapter.
 * <p>
 * In production, this would:
 * 1. Format ISO 8583 messages
 * 2. Send them over a dedicated leased line to NCHL switch
 * 3. Route to the target bank's CBS
 * <p>
 * This mock simulates successful responses for development & testing.
 */
@Component
@Slf4j
public class NchlMockBankAdapter implements BankAdapter {

    @Override
    public String getBankCode() {
        return "NCHL";  // Default adapter for all banks via NCHL switch
    }

    @Override
    public BankResponse debit(String accountNumber, Long amountPaisa, String txnId) {
        log.info("[NCHL-MOCK] DEBIT request: account={}, amount={} paisa, txnId={}",
                maskAccount(accountNumber), amountPaisa, txnId);

        // Simulate processing delay
        simulateLatency();

        // In production: build ISO 8583 MTI 0200 message, send to NCHL gateway
        // Iso8583Message msg = Iso8583Message.builder()
        //     .mti("0200")
        //     .field(2, accountNumber)        // Primary account number
        //     .field(3, "000000")             // Processing code — debit
        //     .field(4, String.format("%012d", amountPaisa))
        //     .field(11, IdGenerator.generateStan())
        //     .field(37, txnId)
        //     .build();
        // return nchlGateway.send(msg);

        String bankRef = "BNK" + IdGenerator.generateStan();
        log.info("[NCHL-MOCK] DEBIT success: txnId={}, bankRef={}", txnId, bankRef);
        return BankResponse.ok(bankRef);
    }

    @Override
    public BankResponse credit(String accountNumber, Long amountPaisa, String txnId) {
        log.info("[NCHL-MOCK] CREDIT request: account={}, amount={} paisa, txnId={}",
                maskAccount(accountNumber), amountPaisa, txnId);

        simulateLatency();

        String bankRef = "BNK" + IdGenerator.generateStan();
        log.info("[NCHL-MOCK] CREDIT success: txnId={}, bankRef={}", txnId, bankRef);
        return BankResponse.ok(bankRef);
    }

    @Override
    public BankResponse checkBalance(String accountNumber) {
        log.info("[NCHL-MOCK] BALANCE CHECK: account={}", maskAccount(accountNumber));
        simulateLatency();
        return BankResponse.ok("BALANCE_OK");
    }

    @Override
    public BankResponse reversal(String originalTxnId) {
        log.info("[NCHL-MOCK] REVERSAL request: originalTxnId={}", originalTxnId);
        simulateLatency();
        String bankRef = "REV" + IdGenerator.generateStan();
        log.info("[NCHL-MOCK] REVERSAL success: originalTxnId={}, bankRef={}", originalTxnId, bankRef);
        return BankResponse.ok(bankRef);
    }

    private void simulateLatency() {
        try {
            Thread.sleep((long) (Math.random() * 200) + 50);  // 50–250ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
