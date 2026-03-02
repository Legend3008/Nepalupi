package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.VpaTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VpaTransferLogRepository extends JpaRepository<VpaTransferLog, UUID> {
    List<VpaTransferLog> findByVpaAddress(String vpaAddress);
    List<VpaTransferLog> findByTransferredBy(UUID userId);
}
