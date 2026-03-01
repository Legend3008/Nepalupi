package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Psp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PspRepository extends JpaRepository<Psp, UUID> {

    Optional<Psp> findByPspIdAndIsActiveTrue(String pspId);

    Optional<Psp> findByPspId(String pspId);
}
