package np.com.nepalupi.service.settlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.SettlementGuaranteeFund;
import np.com.nepalupi.repository.SettlementGuaranteeFundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Settlement Guarantee Fund Service — manages the NRB-mandated fund
 * that guarantees inter-bank UPI settlement.
 * <p>
 * Per RBI/NRB guidelines:
 * - Each participating bank contributes to the fund proportional to UPI volume
 * - The fund covers settlement defaults (if a bank cannot settle its net debit position)
 * - NRB must approve new fund contributions and utilizations
 * - Fund must be replenished if utilization exceeds threshold
 * <p>
 * Key operations:
 * 1. Record bank contributions
 * 2. Utilize fund for settlement default coverage
 * 3. Replenish fund after utilization
 * 4. NRB approval workflow
 * 5. Fund balance queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementGuaranteeFundService {

    private final SettlementGuaranteeFundRepository sgfRepository;

    /**
     * Record a bank's contribution to the Settlement Guarantee Fund.
     *
     * @param bankCode         Bank identifier (e.g. NABIL, SBI, GLOBAL)
     * @param contributionPaisa Amount contributed in paisa
     * @return Created SGF record
     */
    @Transactional
    public SettlementGuaranteeFund recordContribution(String bankCode, Long contributionPaisa) {
        if (contributionPaisa <= 0) {
            throw new IllegalArgumentException("Contribution must be positive");
        }

        // Calculate new total fund for this bank
        Long existingFund = sgfRepository.getAvailableFundByBank(bankCode);
        Long totalFund = (existingFund != null ? existingFund : 0L) + contributionPaisa;

        SettlementGuaranteeFund sgf = SettlementGuaranteeFund.builder()
                .bankCode(bankCode)
                .contributionPaisa(contributionPaisa)
                .fundDate(LocalDate.now())
                .totalFundPaisa(totalFund)
                .utilizationPaisa(0L)
                .status("ACTIVE")
                .nrbApproved(false) // Requires NRB approval
                .build();

        sgf = sgfRepository.save(sgf);
        log.info("SGF contribution recorded: bank={}, contribution={} paisa, total={} paisa",
                bankCode, contributionPaisa, totalFund);
        return sgf;
    }

    /**
     * Utilize fund to cover a settlement default by a bank.
     *
     * @param bankCode        Defaulting bank
     * @param amountPaisa     Amount to utilize
     * @return Updated SGF record
     */
    @Transactional
    public SettlementGuaranteeFund utilizeFund(String bankCode, Long amountPaisa) {
        Long available = sgfRepository.getAvailableFundByBank(bankCode);
        if (available == null || available < amountPaisa) {
            throw new IllegalStateException(
                    String.format("Insufficient SGF for bank %s: available=%d, required=%d",
                            bankCode, available != null ? available : 0, amountPaisa));
        }

        SettlementGuaranteeFund sgf = SettlementGuaranteeFund.builder()
                .bankCode(bankCode)
                .contributionPaisa(0L) // No new contribution, just utilization
                .fundDate(LocalDate.now())
                .totalFundPaisa(available)
                .utilizationPaisa(amountPaisa)
                .status("UTILIZED")
                .nrbApproved(false) // NRB must approve utilization
                .build();

        sgf = sgfRepository.save(sgf);
        log.warn("SGF utilized: bank={}, amount={} paisa, remaining={} paisa",
                bankCode, amountPaisa, available - amountPaisa);
        return sgf;
    }

    /**
     * Replenish fund after utilization — bank must top up to minimum threshold.
     */
    @Transactional
    public SettlementGuaranteeFund replenishFund(String bankCode, Long replenishmentPaisa) {
        Long available = sgfRepository.getAvailableFundByBank(bankCode);
        Long newTotal = (available != null ? available : 0L) + replenishmentPaisa;

        SettlementGuaranteeFund sgf = SettlementGuaranteeFund.builder()
                .bankCode(bankCode)
                .contributionPaisa(replenishmentPaisa)
                .fundDate(LocalDate.now())
                .totalFundPaisa(newTotal)
                .utilizationPaisa(0L)
                .status("REPLENISHED")
                .nrbApproved(false)
                .build();

        sgf = sgfRepository.save(sgf);
        log.info("SGF replenished: bank={}, amount={} paisa, newTotal={} paisa",
                bankCode, replenishmentPaisa, newTotal);
        return sgf;
    }

    /**
     * NRB approves a fund contribution, utilization, or replenishment.
     */
    @Transactional
    public SettlementGuaranteeFund approveByNrb(UUID sgfId) {
        SettlementGuaranteeFund sgf = sgfRepository.findById(sgfId)
                .orElseThrow(() -> new IllegalArgumentException("SGF record not found"));

        sgf.setNrbApproved(true);
        sgf = sgfRepository.save(sgf);
        log.info("SGF record NRB approved: id={}, bank={}, status={}",
                sgfId, sgf.getBankCode(), sgf.getStatus());
        return sgf;
    }

    /**
     * Get available fund balance for a specific bank.
     */
    public Long getAvailableFund(String bankCode) {
        Long fund = sgfRepository.getAvailableFundByBank(bankCode);
        return fund != null ? fund : 0L;
    }

    /**
     * Get total available fund across all banks.
     */
    public Long getTotalAvailableFund() {
        Long total = sgfRepository.getTotalAvailableFund();
        return total != null ? total : 0L;
    }

    /**
     * Get all SGF records for a specific bank.
     */
    public List<SettlementGuaranteeFund> getHistoryByBank(String bankCode) {
        return sgfRepository.findByBankCode(bankCode);
    }

    /**
     * Get all pending (unapproved) SGF records.
     */
    public List<SettlementGuaranteeFund> getPendingApprovals() {
        return sgfRepository.findByNrbApprovedFalse();
    }
}
