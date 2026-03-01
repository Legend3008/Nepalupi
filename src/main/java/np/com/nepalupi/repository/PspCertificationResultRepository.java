package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.PspCertificationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PspCertificationResultRepository extends JpaRepository<PspCertificationResult, UUID> {

    List<PspCertificationResult> findByPspIdOrderByExecutedAtDesc(String pspId);

    List<PspCertificationResult> findByPspIdAndTestSuite(String pspId, String testSuite);

    @Query("SELECT COUNT(r) FROM PspCertificationResult r WHERE r.pspId = :pspId AND r.mandatory = true AND r.passed = true")
    long countMandatoryPassed(@Param("pspId") String pspId);

    @Query("SELECT COUNT(r) FROM PspCertificationResult r WHERE r.pspId = :pspId AND r.mandatory = true")
    long countMandatoryTotal(@Param("pspId") String pspId);

    @Query("SELECT COUNT(r) FROM PspCertificationResult r WHERE r.pspId = :pspId AND r.mandatory = false AND r.passed = true")
    long countAdvisoryPassed(@Param("pspId") String pspId);

    @Query("SELECT COUNT(r) FROM PspCertificationResult r WHERE r.pspId = :pspId AND r.mandatory = false")
    long countAdvisoryTotal(@Param("pspId") String pspId);
}
