package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Tpap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TpapRepository extends JpaRepository<Tpap, UUID> {

    Optional<Tpap> findByTpapId(String tpapId);

    List<Tpap> findBySponsorPspId(UUID sponsorPspId);

    List<Tpap> findByIsActiveTrue();

    List<Tpap> findByStatus(String status);
}
