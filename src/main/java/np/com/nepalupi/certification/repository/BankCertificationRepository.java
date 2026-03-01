package np.com.nepalupi.certification.repository;

import np.com.nepalupi.certification.entity.BankCertification;
import np.com.nepalupi.certification.enums.CertificationStage;
import np.com.nepalupi.certification.enums.CertificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankCertificationRepository extends JpaRepository<BankCertification, UUID> {

    Optional<BankCertification> findByBankCode(String bankCode);

    List<BankCertification> findByStageOrderByCreatedAt(CertificationStage stage);

    List<BankCertification> findByStatusOrderByCreatedAt(CertificationStatus status);

    List<BankCertification> findByStageAndStatusOrderByCreatedAt(CertificationStage stage, CertificationStatus status);
}
