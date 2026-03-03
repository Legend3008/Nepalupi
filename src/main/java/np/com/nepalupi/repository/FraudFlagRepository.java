package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.FraudFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, UUID> {

    List<FraudFlag> findByUserIdAndReviewedFalse(UUID userId);

    List<FraudFlag> findByReviewedFalseOrderByCreatedAtDesc();

    Page<FraudFlag> findByReviewedFalse(Pageable pageable);

    long countByUserIdAndReviewedFalse(UUID userId);

    List<FraudFlag> findByTransactionId(UUID transactionId);

    List<FraudFlag> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
