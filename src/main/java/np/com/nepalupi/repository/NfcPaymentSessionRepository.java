package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.NfcPaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NfcPaymentSessionRepository extends JpaRepository<NfcPaymentSession, UUID> {

    List<NfcPaymentSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<NfcPaymentSession> findByStatus(String status);
}
