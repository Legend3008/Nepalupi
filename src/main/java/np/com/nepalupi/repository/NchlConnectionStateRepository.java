package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.NchlConnectionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NchlConnectionStateRepository extends JpaRepository<NchlConnectionState, Long> {

    Optional<NchlConnectionState> findByChannel(String channel);
}
