package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.IpWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IpWhitelistRepository extends JpaRepository<IpWhitelist, UUID> {
    List<IpWhitelist> findByIsActiveTrue();
    List<IpWhitelist> findByPspIdAndIsActiveTrue(String pspId);
    boolean existsByPspIdAndIpAddress(String pspId, String ipAddress);
}
