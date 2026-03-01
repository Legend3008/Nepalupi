package np.com.nepalupi.certification.repository;

import np.com.nepalupi.certification.entity.CertTestCase;
import np.com.nepalupi.certification.enums.TestCaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertTestCaseRepository extends JpaRepository<CertTestCase, UUID> {

    Optional<CertTestCase> findByTestCode(String testCode);

    List<CertTestCase> findByCategoryOrderByTestCode(TestCaseCategory category);

    List<CertTestCase> findByIsMandatoryTrueOrderByTestCode();

    List<CertTestCase> findByIsMandatoryFalseOrderByTestCode();
}
