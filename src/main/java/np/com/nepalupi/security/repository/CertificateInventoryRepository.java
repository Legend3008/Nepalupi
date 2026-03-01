package np.com.nepalupi.security.repository;

import np.com.nepalupi.security.entity.CertificateInventory;
import np.com.nepalupi.security.enums.CertificateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CertificateInventoryRepository extends JpaRepository<CertificateInventory, UUID> {

    List<CertificateInventory> findByStatusOrderByValidUntil(CertificateStatus status);

    @Query("SELECT c FROM CertificateInventory c WHERE c.validUntil <= :threshold AND c.status = 'ACTIVE' ORDER BY c.validUntil")
    List<CertificateInventory> findExpiringBefore(Instant threshold);

    List<CertificateInventory> findByAssociatedServiceOrderByValidUntil(String service);

    @Query("SELECT c FROM CertificateInventory c WHERE c.autoRotate = true AND c.nextRotationDue <= :now AND c.status = 'ACTIVE'")
    List<CertificateInventory> findDueForAutoRotation(Instant now);
}
