package np.com.nepalupi.operations.repository;

import np.com.nepalupi.operations.entity.Postmortem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostmortemRepository extends JpaRepository<Postmortem, UUID> {

    Optional<Postmortem> findByIncidentId(UUID incidentId);

    List<Postmortem> findByStatusOrderByCreatedAtDesc(String status);
}
