package np.com.nepalupi.merchant.repository;

import np.com.nepalupi.merchant.entity.MerchantSettlement;
import np.com.nepalupi.merchant.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantSettlementRepository extends JpaRepository<MerchantSettlement, UUID> {

    Optional<MerchantSettlement> findByMerchantIdAndSettlementDate(UUID merchantId, LocalDate date);

    List<MerchantSettlement> findBySettlementDateAndSettlementStatus(LocalDate date, SettlementStatus status);

    @Query("SELECT ms FROM MerchantSettlement ms WHERE ms.merchantId = :merchantId " +
           "AND ms.settlementDate BETWEEN :from AND :to ORDER BY ms.settlementDate DESC")
    List<MerchantSettlement> findByMerchantIdAndDateRange(
            @Param("merchantId") UUID merchantId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
