package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.SoundboxDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SoundboxDeviceRepository extends JpaRepository<SoundboxDevice, UUID> {

    List<SoundboxDevice> findByMerchantIdAndIsActiveTrue(UUID merchantId);

    Optional<SoundboxDevice> findByDeviceSerial(String deviceSerial);

    List<SoundboxDevice> findByMerchantId(UUID merchantId);
}
