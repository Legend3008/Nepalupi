package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.SettlementGuaranteeFund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SettlementGuaranteeFundRepository extends JpaRepository<SettlementGuaranteeFund, UUID> {

    List<SettlementGuaranteeFund> findByBankCode(String bankCode);

    List<SettlementGuaranteeFund> findByBankCodeOrderByFundDateDesc(String bankCode);

    List<SettlementGuaranteeFund> findByNrbApprovedFalse();

    List<SettlementGuaranteeFund> findByFundDate(LocalDate date);

    @Query("SELECT SUM(s.contributionPaisa - s.utilizationPaisa) FROM SettlementGuaranteeFund s WHERE s.bankCode = :bankCode AND s.status = 'ACTIVE'")
    Long getAvailableFundByBank(String bankCode);

    @Query("SELECT SUM(s.contributionPaisa - s.utilizationPaisa) FROM SettlementGuaranteeFund s WHERE s.status = 'ACTIVE'")
    Long getTotalAvailableFund();
}
