package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.UpiLiteWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpiLiteWalletRepository extends JpaRepository<UpiLiteWallet, UUID> {
    Optional<UpiLiteWallet> findByUserId(UUID userId);
    Optional<UpiLiteWallet> findByUserIdAndIsActiveTrue(UUID userId);
}
