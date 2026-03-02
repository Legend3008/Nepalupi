package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.InternationalRemittance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InternationalRemittanceRepository extends JpaRepository<InternationalRemittance, UUID> {
    List<InternationalRemittance> findByPayerVpaOrderByCreatedAtDesc(String payerVpa);
    List<InternationalRemittance> findByStatus(String status);
}
