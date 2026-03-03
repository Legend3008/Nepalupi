package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.PushNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PushNotificationRepository extends JpaRepository<PushNotification, UUID> {

    List<PushNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<PushNotification> findByStatus(String status);
}
