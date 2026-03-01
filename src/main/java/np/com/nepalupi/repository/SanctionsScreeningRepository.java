package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.SanctionsScreening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SanctionsScreeningRepository extends JpaRepository<SanctionsScreening, UUID> {

    List<SanctionsScreening> findByUserId(UUID userId);

    List<SanctionsScreening> findByUserIdAndMatchFoundTrue(UUID userId);

    List<SanctionsScreening> findByScreenedAgainst(String screenedAgainst);

    long countByMatchFoundTrue();
}
