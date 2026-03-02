package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.UpiLiteTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UpiLiteTransactionRepository extends JpaRepository<UpiLiteTransaction, UUID> {
    List<UpiLiteTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
    List<UpiLiteTransaction> findBySettledFalse();
}
