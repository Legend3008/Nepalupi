package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, UUID> {

    List<EmailNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<EmailNotification> findByStatus(String status);
}
