package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    List<Beneficiary> findByUserIdOrderByLastPaidAtDesc(UUID userId);

    List<Beneficiary> findByUserIdAndIsFavoriteTrueOrderByBeneficiaryName(UUID userId);

    Optional<Beneficiary> findByUserIdAndBeneficiaryVpa(UUID userId, String beneficiaryVpa);

    List<Beneficiary> findTop10ByUserIdOrderByTransactionCountDesc(UUID userId);
}
