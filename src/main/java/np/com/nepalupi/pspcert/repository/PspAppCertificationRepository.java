package np.com.nepalupi.pspcert.repository;

import np.com.nepalupi.pspcert.entity.PspAppCertification;
import np.com.nepalupi.pspcert.enums.AppCertificationStage;
import np.com.nepalupi.pspcert.enums.AppCertificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PspAppCertificationRepository extends JpaRepository<PspAppCertification, UUID> {

    List<PspAppCertification> findByPspIdOrderByCreatedAtDesc(String pspId);

    List<PspAppCertification> findByStageOrderByCreatedAt(AppCertificationStage stage);

    List<PspAppCertification> findByStatusOrderByCreatedAt(AppCertificationStatus status);

    List<PspAppCertification> findByStageAndStatusOrderByCreatedAt(AppCertificationStage stage, AppCertificationStatus status);
}
