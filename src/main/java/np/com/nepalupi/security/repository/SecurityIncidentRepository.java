package np.com.nepalupi.security.repository;

import np.com.nepalupi.security.entity.SecurityIncident;
import np.com.nepalupi.security.enums.SecurityIncidentStatus;
import np.com.nepalupi.security.enums.SecurityIncidentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, UUID> {

    Optional<SecurityIncident> findByIncidentReference(String reference);

    List<SecurityIncident> findByStatusOrderByDetectedAtDesc(SecurityIncidentStatus status);

    List<SecurityIncident> findByIncidentTypeOrderByDetectedAtDesc(SecurityIncidentType type);

    @Query("SELECT i FROM SecurityIncident i WHERE i.status NOT IN ('RECOVERED', 'CLOSED') ORDER BY i.detectedAt DESC")
    List<SecurityIncident> findActiveIncidents();

    @Query("SELECT i FROM SecurityIncident i WHERE i.dataCompromised = true AND i.usersNotified = false")
    List<SecurityIncident> findBreachesWithUnnotifiedUsers();
}
