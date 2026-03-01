package np.com.nepalupi.operations.repository;

import np.com.nepalupi.operations.entity.Incident;
import np.com.nepalupi.operations.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByIncidentNumber(String incidentNumber);

    List<Incident> findByStatusOrderByDetectedAtDesc(IncidentStatus status);

    List<Incident> findBySeverityOrderByDetectedAtDesc(Integer severity);

    @Query("SELECT i FROM Incident i WHERE i.status NOT IN ('RESOLVED', 'POSTMORTEM_PENDING') ORDER BY i.severity ASC, i.detectedAt ASC")
    List<Incident> findActiveIncidents();

    @Query("SELECT i FROM Incident i WHERE i.detectedAt BETWEEN :from AND :to ORDER BY i.detectedAt DESC")
    List<Incident> findByDateRange(Instant from, Instant to);

    long countByStatusIn(List<IncidentStatus> statuses);
}
