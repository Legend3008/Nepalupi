package np.com.nepalupi.registration.repository;

import np.com.nepalupi.registration.entity.UserKyc;
import np.com.nepalupi.registration.enums.KycLevel;
import np.com.nepalupi.registration.enums.KycVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserKycRepository extends JpaRepository<UserKyc, UUID> {

    Optional<UserKyc> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserKyc> findByUserIdAndKycLevel(UUID userId, KycLevel kycLevel);

    List<UserKyc> findByVerificationStatus(KycVerificationStatus status);

    Optional<UserKyc> findByDocumentTypeAndDocumentNumber(
            np.com.nepalupi.registration.enums.KycDocumentType documentType, String documentNumber);

    boolean existsByUserIdAndVerificationStatus(UUID userId, KycVerificationStatus status);
}
