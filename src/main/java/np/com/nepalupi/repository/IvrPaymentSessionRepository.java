package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.IvrPaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IvrPaymentSessionRepository extends JpaRepository<IvrPaymentSession, UUID> {

    List<IvrPaymentSession> findByCallerMobileOrderByCreatedAtDesc(String callerMobile);

    List<IvrPaymentSession> findByStatus(String status);
}
