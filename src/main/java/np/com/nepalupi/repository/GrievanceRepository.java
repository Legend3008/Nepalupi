package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Grievance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, UUID> {

    List<Grievance> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<Grievance> findByStatus(String status, Pageable pageable);

    Optional<Grievance> findByTicketNumber(String ticketNumber);

    List<Grievance> findByStatusAndEscalationLevelGreaterThanEqual(String status, Integer level);

    long countByUserIdAndStatus(UUID userId, String status);
}
