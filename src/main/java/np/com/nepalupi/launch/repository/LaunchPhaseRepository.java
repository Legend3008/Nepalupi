package np.com.nepalupi.launch.repository;

import np.com.nepalupi.launch.entity.LaunchPhase;
import np.com.nepalupi.launch.enums.LaunchPhaseName;
import np.com.nepalupi.launch.enums.LaunchPhaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LaunchPhaseRepository extends JpaRepository<LaunchPhase, UUID> {

    Optional<LaunchPhase> findByPhaseName(LaunchPhaseName phaseName);

    List<LaunchPhase> findByStatusOrderByPhaseNumberAsc(LaunchPhaseStatus status);

    List<LaunchPhase> findAllByOrderByPhaseNumberAsc();

    Optional<LaunchPhase> findByStatus(LaunchPhaseStatus status);
}
