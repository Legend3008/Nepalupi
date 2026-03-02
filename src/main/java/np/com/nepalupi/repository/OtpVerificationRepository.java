package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    @Query("SELECT o FROM OtpVerification o WHERE o.mobileNumber = :mobile " +
           "AND o.purpose = :purpose AND o.expiresAt > :now AND o.isVerified = false " +
           "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findLatestActiveOtp(@Param("mobile") String mobileNumber,
                                                    @Param("purpose") String purpose,
                                                    @Param("now") Instant now);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.isVerified = false, o.expiresAt = :now " +
           "WHERE o.mobileNumber = :mobile AND o.purpose = :purpose AND o.isVerified = false AND o.expiresAt > :now")
    void invalidateExisting(@Param("mobile") String mobileNumber,
                            @Param("purpose") String purpose,
                            @Param("now") Instant now);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OtpVerification o " +
           "WHERE o.mobileNumber = :mobile AND o.purpose = :purpose AND o.isVerified = true " +
           "AND o.verifiedAt > :since")
    boolean existsVerifiedOtp(@Param("mobile") String mobileNumber,
                               @Param("purpose") String purpose,
                               @Param("since") Instant since);
}
