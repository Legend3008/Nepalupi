package np.com.nepalupi.mandate.repository;

import np.com.nepalupi.mandate.entity.MandateNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MandateNotificationRepository extends JpaRepository<MandateNotification, UUID> {
    List<MandateNotification> findByMandateId(UUID mandateId);
    List<MandateNotification> findByNotificationType(String notificationType);
}
