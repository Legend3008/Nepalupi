package np.com.nepalupi.merchant.repository;

import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.enums.MerchantStatus;
import np.com.nepalupi.merchant.enums.MerchantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByMerchantId(String merchantId);

    Optional<Merchant> findByMerchantVpa(String merchantVpa);

    Optional<Merchant> findByUserId(UUID userId);

    List<Merchant> findByMerchantTypeAndStatus(MerchantType type, MerchantStatus status);

    List<Merchant> findByStatus(MerchantStatus status);

    boolean existsByMerchantVpa(String merchantVpa);
}
