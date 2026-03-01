package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.DisputeActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeActionLogRepository extends JpaRepository<DisputeActionLog, Long> {

    List<DisputeActionLog> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);
}
