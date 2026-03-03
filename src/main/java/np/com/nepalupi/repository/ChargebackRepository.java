package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Chargeback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChargebackRepository extends JpaRepository<Chargeback, UUID> {

    Optional<Chargeback> findByDisputeId(UUID disputeId);

    List<Chargeback> findByTransactionId(UUID transactionId);

    Page<Chargeback> findByStatus(String status, Pageable pageable);

    List<Chargeback> findByStatusIn(List<String> statuses);
}
