package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.MobileRecharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MobileRechargeRepository extends JpaRepository<MobileRecharge, UUID> {

    List<MobileRecharge> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<MobileRecharge> findByMobileNumber(String mobileNumber);

    List<MobileRecharge> findByStatus(String status);
}
