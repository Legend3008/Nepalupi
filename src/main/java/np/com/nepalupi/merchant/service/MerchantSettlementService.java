package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.response.BankResponse;
import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.entity.MerchantSettlement;
import np.com.nepalupi.merchant.enums.MerchantStatus;
import np.com.nepalupi.merchant.enums.MerchantType;
import np.com.nepalupi.merchant.enums.SettlementStatus;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import np.com.nepalupi.merchant.repository.MerchantSettlementRepository;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.service.bank.BankConnector;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    private static final ZoneId NPT = ZoneId.of("Asia/Kathmandu");

    private final MerchantSettlementRepository settlementRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final BankConnector bankConnector;

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

        // Query actual transactions for this merchant on this date
        long totalAmount = calculateDailyTotal(merchant, date);
        int txnCount = calculateDailyCount(merchant, date);

        if (txnCount == 0) {
            log.debug("No transactions for merchant={} on date={}", merchant.getMerchantId(), date);
            return existing.orElse(null);
        }

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

        // Credit merchant's settlement bank account via NCHL bank connector
        if (merchant.getSettlementAccountId() != null) {
            String bankCode = merchant.getMerchantVpa() != null
                    ? merchant.getMerchantVpa().split("@")[1].toUpperCase() : "NCHL";
            try {
                BankResponse creditResponse = bankConnector.credit(
                        bankCode, merchant.getSettlementAccountId().toString(), netAmount, settlement.getSettlementReference());
                if (creditResponse.isSuccess()) {
                    settlement.setSettlementStatus(SettlementStatus.SETTLED);
                    settlement.setSettledAt(Instant.now());
                    log.info("Settlement credit successful for merchant={} ref={}",
                            merchant.getMerchantId(), settlement.getSettlementReference());
                } else {
                    settlement.setSettlementStatus(SettlementStatus.FAILED);
                    log.warn("Settlement credit failed for merchant={}: {}",
                            merchant.getMerchantId(), creditResponse.getErrorMessage());
                }
            } catch (Exception e) {
                settlement.setSettlementStatus(SettlementStatus.FAILED);
                log.error("Settlement credit exception for merchant={}: {}",
                        merchant.getMerchantId(), e.getMessage());
            }
        } else {
            settlement.setSettlementStatus(SettlementStatus.SETTLED);
            settlement.setSettledAt(Instant.now());
        }

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
        if (merchant.getMerchantVpa() == null) return 0L;
        Instant dayStart = date.atStartOfDay(NPT).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(NPT).toInstant();
        return transactionRepository.findByPayeeVpaOrderByInitiatedAtDesc(merchant.getMerchantVpa())
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> t.getCompletedAt() != null
                        && !t.getCompletedAt().isBefore(dayStart)
                        && t.getCompletedAt().isBefore(dayEnd))
                .mapToLong(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();
    }

    private int calculateDailyCount(Merchant merchant, LocalDate date) {
        if (merchant.getMerchantVpa() == null) return 0;
        Instant dayStart = date.atStartOfDay(NPT).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(NPT).toInstant();
        return (int) transactionRepository.findByPayeeVpaOrderByInitiatedAtDesc(merchant.getMerchantVpa())
                .stream()
                .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                .filter(t -> t.getCompletedAt() != null
                        && !t.getCompletedAt().isBefore(dayStart)
                        && t.getCompletedAt().isBefore(dayEnd))
                .count();
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
