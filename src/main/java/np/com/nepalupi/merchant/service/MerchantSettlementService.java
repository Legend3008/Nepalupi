package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.entity.MerchantSettlement;
import np.com.nepalupi.merchant.enums.MerchantStatus;
import np.com.nepalupi.merchant.enums.MerchantType;
import np.com.nepalupi.merchant.enums.SettlementStatus;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import np.com.nepalupi.merchant.repository.MerchantSettlementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Merchant Settlement Service — daily settlement cycle.
 * <p>
 * Indian UPI model:
 * - Zero MDR for P2P and small merchant transactions (below Rs 2,000)
 * - T+0 (same day) or T+1 (next business day) settlement
 * - Settlement runs as a batch job — aggregates all transactions per merchant
 *   per day, deducts MDR, credits net amount to settlement account
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantSettlementService {

    private final MerchantSettlementRepository settlementRepository;
    private final MerchantRepository merchantRepository;

    /**
     * Daily settlement job — runs at 2 AM every day.
     * Settles T-1 transactions for T+1 merchants.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void runDailySettlement() {
        LocalDate settlementDate = LocalDate.now().minusDays(1);
        log.info("Running daily merchant settlement for date={}", settlementDate);

        List<Merchant> activeMerchants = merchantRepository.findByStatus(MerchantStatus.ACTIVE);

        int settled = 0;
        for (Merchant merchant : activeMerchants) {
            try {
                settleForMerchant(merchant, settlementDate);
                settled++;
            } catch (Exception e) {
                log.error("Settlement failed for merchant={}: {}", merchant.getMerchantId(), e.getMessage());
            }
        }

        log.info("Daily settlement completed: {}/{} merchants settled", settled, activeMerchants.size());
    }

    /**
     * Settle a specific merchant for a specific date.
     */
    @Transactional
    public MerchantSettlement settleForMerchant(Merchant merchant, LocalDate date) {
        // Check if already settled
        var existing = settlementRepository.findByMerchantIdAndSettlementDate(merchant.getId(), date);
        if (existing.isPresent() && existing.get().getSettlementStatus() == SettlementStatus.SETTLED) {
            return existing.get();
        }

        // In production: query transaction table for merchant's received transactions on this date
        // Simulated: create settlement record
        long totalAmount = calculateDailyTotal(merchant, date);
        int txnCount = calculateDailyCount(merchant, date);

        long mdrDeducted = calculateMdr(totalAmount, merchant.getMdrPercent());
        long netAmount = totalAmount - mdrDeducted;

        MerchantSettlement settlement = existing.orElseGet(() ->
                MerchantSettlement.builder()
                        .merchantId(merchant.getId())
                        .settlementDate(date)
                        .build());

        settlement.setTotalTxnCount(txnCount);
        settlement.setTotalAmountPaisa(totalAmount);
        settlement.setMdrDeductedPaisa(mdrDeducted);
        settlement.setNetAmountPaisa(netAmount);
        settlement.setSettlementReference("STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // In production: initiate credit to merchant's settlement bank account via NCHL
        settlement.setSettlementStatus(SettlementStatus.SETTLED);
        settlement.setSettledAt(Instant.now());

        settlement = settlementRepository.save(settlement);

        log.info("Settled merchant={} date={} gross={} mdr={} net={}",
                merchant.getMerchantId(), date, totalAmount, mdrDeducted, netAmount);
        return settlement;
    }

    /**
     * Get settlement history for a merchant.
     */
    public List<MerchantSettlement> getSettlementHistory(UUID merchantDbId, LocalDate from, LocalDate to) {
        return settlementRepository.findByMerchantIdAndDateRange(merchantDbId, from, to);
    }

    // ── Helpers ──

    private long calculateDailyTotal(Merchant merchant, LocalDate date) {
        // In production: SUM(amount) FROM transactions WHERE payee_vpa = merchant.vpa AND date = date
        return 0L; // placeholder
    }

    private int calculateDailyCount(Merchant merchant, LocalDate date) {
        // In production: COUNT(*) FROM transactions WHERE payee_vpa = merchant.vpa AND date = date
        return 0; // placeholder
    }

    private long calculateMdr(long totalAmountPaisa, BigDecimal mdrPercent) {
        if (mdrPercent == null || mdrPercent.compareTo(BigDecimal.ZERO) == 0) {
            return 0L;
        }
        return BigDecimal.valueOf(totalAmountPaisa)
                .multiply(mdrPercent)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
