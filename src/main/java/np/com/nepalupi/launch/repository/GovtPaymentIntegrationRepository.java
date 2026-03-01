package np.com.nepalupi.launch.repository;

import np.com.nepalupi.launch.entity.GovtPaymentIntegration;
import np.com.nepalupi.launch.enums.GovtIntegrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GovtPaymentIntegrationRepository extends JpaRepository<GovtPaymentIntegration, UUID> {

    Optional<GovtPaymentIntegration> findByAgencyCode(String agencyCode);

    List<GovtPaymentIntegration> findByIntegrationStatus(GovtIntegrationStatus status);

    List<GovtPaymentIntegration> findByIntegrationStatusOrderByAgencyNameAsc(GovtIntegrationStatus status);

    List<GovtPaymentIntegration> findByPaymentType(String paymentType);
}
