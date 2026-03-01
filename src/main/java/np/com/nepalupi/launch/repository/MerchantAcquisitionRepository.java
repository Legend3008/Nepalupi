package np.com.nepalupi.launch.repository;

import np.com.nepalupi.launch.entity.MerchantAcquisition;
import np.com.nepalupi.launch.enums.AcquisitionChannel;
import np.com.nepalupi.launch.enums.FootfallCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantAcquisitionRepository extends JpaRepository<MerchantAcquisition, UUID> {

    Optional<MerchantAcquisition> findByMerchantId(UUID merchantId);

    List<MerchantAcquisition> findByCity(String city);

    List<MerchantAcquisition> findByFootfallCategory(FootfallCategory footfallCategory);

    List<MerchantAcquisition> findByAcquisitionChannel(AcquisitionChannel channel);

    List<MerchantAcquisition> findByIsActiveTrue();

    @Query("SELECT m FROM MerchantAcquisition m WHERE m.qrDeployed = true AND m.firstTransactionAt IS NULL")
    List<MerchantAcquisition> findQrDeployedButNoTransaction();

    @Query("SELECT m FROM MerchantAcquisition m WHERE m.churnedAt IS NOT NULL")
    List<MerchantAcquisition> findChurnedMerchants();

    @Query("SELECT COUNT(m) FROM MerchantAcquisition m WHERE m.isActive = true")
    long countActiveMerchants();

    @Query("SELECT m.city, COUNT(m) FROM MerchantAcquisition m WHERE m.isActive = true GROUP BY m.city ORDER BY COUNT(m) DESC")
    List<Object[]> countActiveMerchantsByCity();
}
