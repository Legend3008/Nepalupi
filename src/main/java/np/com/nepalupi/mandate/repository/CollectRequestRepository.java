package np.com.nepalupi.mandate.repository;

import np.com.nepalupi.mandate.entity.CollectRequest;
import np.com.nepalupi.mandate.enums.CollectRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectRequestRepository extends JpaRepository<CollectRequest, UUID> {

    Optional<CollectRequest> findByCollectRef(String collectRef);

    List<CollectRequest> findByPayerVpaAndStatusOrderByCreatedAtDesc(String payerVpa, CollectRequestStatus status);

    List<CollectRequest> findByRequestorVpaOrderByCreatedAtDesc(String requestorVpa);

    @Query("SELECT cr FROM CollectRequest cr WHERE cr.status = 'PENDING' AND cr.expiresAt < :now")
    List<CollectRequest> findExpiredPendingRequests(Instant now);
}
