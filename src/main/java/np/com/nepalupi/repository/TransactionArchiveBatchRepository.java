package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.TransactionArchiveBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionArchiveBatchRepository extends JpaRepository<TransactionArchiveBatch, UUID> {
    long countByStatus(String status);
}
