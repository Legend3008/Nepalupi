package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.config.FeatureFlagService;
import np.com.nepalupi.domain.entity.UpiLiteTransaction;
import np.com.nepalupi.domain.entity.UpiLiteWallet;
import np.com.nepalupi.repository.UpiLiteTransactionRepository;
import np.com.nepalupi.repository.UpiLiteWalletRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Section 19.1: UPI Lite — small-value payments without PIN.
 * <p>
 * - Payments under NPR 500 (50,000 paisa) without UPI PIN
 * - Pre-loaded on-device wallet (max NPR 2,000 balance)
 * - Settlement with bank happens in batch
 * - Load wallet from linked bank account (requires PIN)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpiLiteService {

    private final UpiLiteWalletRepository walletRepository;
    private final UpiLiteTransactionRepository txnRepository;
    private final FeatureFlagService featureFlagService;

    private static final long DEFAULT_MAX_BALANCE = 200_000L;   // NPR 2,000
    private static final long DEFAULT_PER_TXN_LIMIT = 50_000L;  // NPR 500

    /**
     * Enable UPI Lite for a user — create wallet.
     */
    @Transactional
    public UpiLiteWallet enableUpiLite(UUID userId, UUID bankAccountId, String bankCode) {
        checkFeatureEnabled();

        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("UPI Lite already enabled for this user");
        }

        UpiLiteWallet wallet = UpiLiteWallet.builder()
                .userId(userId)
                .balancePaisa(0L)
                .maxBalancePaisa(DEFAULT_MAX_BALANCE)
                .perTxnLimitPaisa(DEFAULT_PER_TXN_LIMIT)
                .isActive(true)
                .linkedBankAccountId(bankAccountId)
                .linkedBankCode(bankCode)
                .build();

        wallet = walletRepository.save(wallet);
        log.info("UPI Lite enabled for user={}, wallet={}", userId, wallet.getId());
        return wallet;
    }

    /**
     * Load wallet from linked bank account.
     * This requires UPI PIN verification (handled by caller).
     */
    @Transactional
    public Map<String, Object> loadWallet(UUID userId, long amountPaisa) {
        checkFeatureEnabled();

        UpiLiteWallet wallet = getActiveWallet(userId);

        long newBalance = wallet.getBalancePaisa() + amountPaisa;
        if (newBalance > wallet.getMaxBalancePaisa()) {
            throw new IllegalStateException(String.format(
                    "Loading NPR %.2f would exceed max balance of NPR %.2f. Current: NPR %.2f",
                    amountPaisa / 100.0, wallet.getMaxBalancePaisa() / 100.0, wallet.getBalancePaisa() / 100.0));
        }

        wallet.setBalancePaisa(newBalance);
        walletRepository.save(wallet);

        // Record load transaction
        UpiLiteTransaction txn = UpiLiteTransaction.builder()
                .walletId(wallet.getId())
                .txnType("LOAD")
                .amountPaisa(amountPaisa)
                .status("COMPLETED")
                .settled(false)
                .description("Wallet load from bank account")
                .build();
        txnRepository.save(txn);

        log.info("UPI Lite wallet loaded: user={}, amount={}, newBalance={}", userId, amountPaisa, newBalance);
        return Map.of(
                "status", "SUCCESS",
                "walletBalance", newBalance,
                "amountLoaded", amountPaisa
        );
    }

    /**
     * Make a UPI Lite payment — NO PIN required.
     * Only for small-value payments under per-txn limit.
     */
    @Transactional
    public Map<String, Object> pay(UUID userId, String payerVpa, String payeeVpa, long amountPaisa, String description) {
        checkFeatureEnabled();

        UpiLiteWallet wallet = getActiveWallet(userId);

        // Per-transaction limit check
        if (amountPaisa > wallet.getPerTxnLimitPaisa()) {
            throw new IllegalStateException(String.format(
                    "Amount NPR %.2f exceeds UPI Lite per-transaction limit of NPR %.2f. Use regular UPI.",
                    amountPaisa / 100.0, wallet.getPerTxnLimitPaisa() / 100.0));
        }

        // Balance check
        if (wallet.getBalancePaisa() < amountPaisa) {
            throw new IllegalStateException(String.format(
                    "Insufficient UPI Lite balance. Available: NPR %.2f, Required: NPR %.2f",
                    wallet.getBalancePaisa() / 100.0, amountPaisa / 100.0));
        }

        // Debit wallet
        wallet.setBalancePaisa(wallet.getBalancePaisa() - amountPaisa);
        walletRepository.save(wallet);

        // Record transaction
        UpiLiteTransaction txn = UpiLiteTransaction.builder()
                .walletId(wallet.getId())
                .txnType("PAY")
                .amountPaisa(amountPaisa)
                .payerVpa(payerVpa)
                .payeeVpa(payeeVpa)
                .description(description)
                .status("COMPLETED")
                .settled(false)
                .build();
        txnRepository.save(txn);

        log.info("UPI Lite payment: {}->{}, amount={}, balance={}", payerVpa, payeeVpa, amountPaisa, wallet.getBalancePaisa());
        return Map.of(
                "status", "SUCCESS",
                "txnId", txn.getId().toString(),
                "amountPaid", amountPaisa,
                "walletBalance", wallet.getBalancePaisa(),
                "pinRequired", false
        );
    }

    /**
     * Get wallet balance and details.
     */
    public Map<String, Object> getWalletDetails(UUID userId) {
        UpiLiteWallet wallet = getActiveWallet(userId);
        return Map.of(
                "walletId", wallet.getId(),
                "balance", wallet.getBalancePaisa(),
                "maxBalance", wallet.getMaxBalancePaisa(),
                "perTxnLimit", wallet.getPerTxnLimitPaisa(),
                "active", wallet.getIsActive()
        );
    }

    /**
     * Get wallet transaction history.
     */
    public List<UpiLiteTransaction> getTransactionHistory(UUID userId) {
        UpiLiteWallet wallet = getActiveWallet(userId);
        return txnRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }

    /**
     * Disable UPI Lite — refund remaining balance to bank account.
     */
    @Transactional
    public Map<String, Object> disableUpiLite(UUID userId) {
        UpiLiteWallet wallet = getActiveWallet(userId);
        long refundAmount = wallet.getBalancePaisa();

        wallet.setIsActive(false);
        wallet.setBalancePaisa(0L);
        walletRepository.save(wallet);

        if (refundAmount > 0) {
            UpiLiteTransaction txn = UpiLiteTransaction.builder()
                    .walletId(wallet.getId())
                    .txnType("REFUND")
                    .amountPaisa(refundAmount)
                    .status("COMPLETED")
                    .settled(false)
                    .description("Wallet closure refund to bank account")
                    .build();
            txnRepository.save(txn);
        }

        log.info("UPI Lite disabled for user={}, refund={}", userId, refundAmount);
        return Map.of(
                "status", "DISABLED",
                "refundAmount", refundAmount,
                "message", "UPI Lite wallet closed. Balance refunded to bank account."
        );
    }

    /**
     * Batch settlement of UPI Lite transactions with banks.
     * Runs every 2 hours.
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    @Transactional
    public void batchSettlement() {
        List<UpiLiteTransaction> unsettled = txnRepository.findBySettledFalse();
        if (unsettled.isEmpty()) return;

        String batchId = "LITE-" + System.currentTimeMillis();
        int count = 0;

        for (UpiLiteTransaction txn : unsettled) {
            txn.setSettled(true);
            txn.setSettlementBatchId(batchId);
            txnRepository.save(txn);
            count++;
        }

        log.info("UPI Lite settlement batch: batchId={}, txnCount={}", batchId, count);
    }

    private UpiLiteWallet getActiveWallet(UUID userId) {
        return walletRepository.findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new IllegalStateException("UPI Lite is not enabled for this user"));
    }

    private void checkFeatureEnabled() {
        if (!featureFlagService.isUpiLiteEnabled()) {
            throw new IllegalStateException("UPI Lite is not yet available. Feature coming soon.");
        }
    }
}
