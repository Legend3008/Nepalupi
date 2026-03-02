package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.DeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, UUID> {
    List<DeadLetterEvent> findByStatus(String status);
    List<DeadLetterEvent> findByTopicAndStatus(String topic, String status);
    long countByStatus(String status);
}
