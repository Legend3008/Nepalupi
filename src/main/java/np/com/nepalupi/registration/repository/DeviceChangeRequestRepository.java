package np.com.nepalupi.registration.repository;

import np.com.nepalupi.registration.entity.DeviceChangeRequest;
import np.com.nepalupi.registration.enums.DeviceChangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceChangeRequestRepository extends JpaRepository<DeviceChangeRequest, UUID> {

    List<DeviceChangeRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<DeviceChangeRequest> findByUserIdAndStatus(UUID userId, DeviceChangeStatus status);

    boolean existsByUserIdAndStatusIn(UUID userId, List<DeviceChangeStatus> statuses);
}
