package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.TransactionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionAuditLogRepository extends JpaRepository<TransactionAuditLog, Long> {

    List<TransactionAuditLog> findByTransactionIdOrderByChangedAtAsc(UUID transactionId);
}
