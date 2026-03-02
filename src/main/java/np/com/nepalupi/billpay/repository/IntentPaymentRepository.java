package np.com.nepalupi.billpay.repository;

import np.com.nepalupi.billpay.entity.IntentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntentPaymentRepository extends JpaRepository<IntentPayment, UUID> {

    Optional<IntentPayment> findByIntentRef(String intentRef);

    List<IntentPayment> findByMerchantVpaOrderByCreatedAtDesc(String merchantVpa);

    @Query("SELECT i FROM IntentPayment i WHERE i.status = 'CREATED' AND i.expiresAt < :now")
    List<IntentPayment> findExpiredIntents(Instant now);
}
