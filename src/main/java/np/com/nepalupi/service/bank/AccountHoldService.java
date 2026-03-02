package np.com.nepalupi.service.bank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Account Hold/Lien Service — manages temporary holds on bank accounts
 * during the debit-credit window of a UPI transaction.
 * <p>
 * Section 3.4: Account management (balance, holds)
 * <p>
 * In the UPI flow, when a debit is initiated:
 * 1. A hold (lien) is placed on the payer's account for the transaction amount
 * 2. If credit succeeds, the hold is released and debit is finalized
 * 3. If credit fails, the hold is released and funds are restored
 * <p>
 * This prevents double-spending during the debit-credit window (typically 1-5 seconds).
 * <p>
 * PRODUCTION NOTE: In production, holds are managed by the bank's CBS (Core Banking System).
 * This service provides the switch-side tracking of holds for visibility and reconciliation.
 * The actual fund blocking is done by the bank's CBS via ISO 8583 messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountHoldService {

    /**
     * In-memory hold tracking for reconciliation.
     * In production: persisted to database with bank confirmation status.
     */
    private final Map<String, HoldRecord> activeHolds = new ConcurrentHashMap<>();

    /**
     * Place a hold on an account for a transaction amount.
     * Called before the debit instruction is sent to the bank.
     *
     * @param accountNumber Payer's account number
     * @param bankCode      Payer's bank code
     * @param amountPaisa   Amount to hold
     * @param txnId         UPI transaction ID
     * @return Hold reference ID
     */
    public String placeHold(String accountNumber, String bankCode,
                             Long amountPaisa, String txnId) {
        String holdRef = "HOLD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        HoldRecord hold = new HoldRecord(
                holdRef, accountNumber, bankCode, amountPaisa, txnId,
                HoldStatus.ACTIVE, Instant.now()
        );

        activeHolds.put(holdRef, hold);
        log.info("Account hold placed: ref={}, account=****{}, bank={}, amount={} paisa, txn={}",
                holdRef,
                accountNumber.length() > 4 ? accountNumber.substring(accountNumber.length() - 4) : accountNumber,
                bankCode, amountPaisa, txnId);

        return holdRef;
    }

    /**
     * Release a hold after successful debit finalization.
     * Called when the full transaction (debit + credit) completes successfully.
     *
     * @param holdRef Hold reference ID
     */
    public void releaseHold(String holdRef) {
        HoldRecord hold = activeHolds.get(holdRef);
        if (hold != null) {
            hold = new HoldRecord(hold.holdRef, hold.accountNumber, hold.bankCode,
                    hold.amountPaisa, hold.txnId, HoldStatus.RELEASED, hold.createdAt);
            activeHolds.put(holdRef, hold);
            log.info("Account hold released: ref={}, txn={}", holdRef, hold.txnId);
        } else {
            log.warn("Hold not found for release: {}", holdRef);
        }
    }

    /**
     * Reverse a hold — restore funds to the account.
     * Called when credit fails and debit must be reversed.
     *
     * @param holdRef Hold reference ID
     */
    public void reverseHold(String holdRef) {
        HoldRecord hold = activeHolds.get(holdRef);
        if (hold != null) {
            hold = new HoldRecord(hold.holdRef, hold.accountNumber, hold.bankCode,
                    hold.amountPaisa, hold.txnId, HoldStatus.REVERSED, hold.createdAt);
            activeHolds.put(holdRef, hold);
            log.info("Account hold reversed: ref={}, txn={}, amount={} paisa restored",
                    holdRef, hold.txnId, hold.amountPaisa);
        } else {
            log.warn("Hold not found for reversal: {}", holdRef);
        }
    }

    /**
     * Check if a hold is active for a given transaction.
     */
    public boolean hasActiveHold(String txnId) {
        return activeHolds.values().stream()
                .anyMatch(h -> h.txnId.equals(txnId) && h.status == HoldStatus.ACTIVE);
    }

    /**
     * Get hold details by reference.
     */
    public HoldRecord getHold(String holdRef) {
        return activeHolds.get(holdRef);
    }

    /**
     * Count active holds (for monitoring).
     */
    public long countActiveHolds() {
        return activeHolds.values().stream()
                .filter(h -> h.status == HoldStatus.ACTIVE)
                .count();
    }

    // ── Data Types ──

    public enum HoldStatus {
        ACTIVE,     // Funds are held/blocked
        RELEASED,   // Transaction completed, hold finalized
        REVERSED,   // Transaction failed, funds restored
        EXPIRED     // Hold timed out
    }

    public record HoldRecord(
            String holdRef,
            String accountNumber,
            String bankCode,
            Long amountPaisa,
            String txnId,
            HoldStatus status,
            Instant createdAt
    ) {}
}
