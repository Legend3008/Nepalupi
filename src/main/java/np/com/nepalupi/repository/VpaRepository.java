package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Vpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VpaRepository extends JpaRepository<Vpa, UUID> {

    Optional<Vpa> findByVpaAddressAndIsActiveTrue(String vpaAddress);

    Optional<Vpa> findByVpaAddress(String vpaAddress);

    List<Vpa> findByUserIdAndIsActiveTrue(UUID userId);

    boolean existsByVpaAddress(String vpaAddress);
}
