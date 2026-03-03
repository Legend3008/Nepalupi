package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.CreditOnUpiCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CreditOnUpiCardRepository extends JpaRepository<CreditOnUpiCard, UUID> {

    List<CreditOnUpiCard> findByUserIdAndIsActiveTrue(UUID userId);

    List<CreditOnUpiCard> findByUserId(UUID userId);

    List<CreditOnUpiCard> findByLinkedVpa(String linkedVpa);
}
