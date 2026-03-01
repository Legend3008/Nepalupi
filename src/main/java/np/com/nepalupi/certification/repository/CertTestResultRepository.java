package np.com.nepalupi.certification.repository;

import np.com.nepalupi.certification.entity.CertTestResult;
import np.com.nepalupi.certification.enums.ExecutionPhase;
import np.com.nepalupi.certification.enums.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertTestResultRepository extends JpaRepository<CertTestResult, UUID> {

    List<CertTestResult> findByCertificationIdAndExecutionPhaseOrderByExecutedAt(UUID certificationId, ExecutionPhase phase);

    List<CertTestResult> findByCertificationIdOrderByExecutedAt(UUID certificationId);

    @Query("SELECT r.result, COUNT(r) FROM CertTestResult r WHERE r.certificationId = :certId AND r.executionPhase = :phase GROUP BY r.result")
    List<Object[]> countResultsByPhase(UUID certId, ExecutionPhase phase);

    long countByCertificationIdAndExecutionPhaseAndResult(UUID certificationId, ExecutionPhase phase, TestResult result);
}
