package np.com.nepalupi.registration.repository;

import np.com.nepalupi.registration.entity.DeviceBinding;
import np.com.nepalupi.registration.enums.DeviceBindingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceBindingRepository extends JpaRepository<DeviceBinding, UUID> {

    Optional<DeviceBinding> findByMobileNumberAndStatus(String mobileNumber, DeviceBindingStatus status);

    Optional<DeviceBinding> findByBindingSmsId(String bindingSmsId);

    List<DeviceBinding> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<DeviceBinding> findByMobileNumberAndDeviceIdAndStatus(
            String mobileNumber, String deviceId, DeviceBindingStatus status);

    boolean existsByMobileNumberAndStatus(String mobileNumber, DeviceBindingStatus status);
}
