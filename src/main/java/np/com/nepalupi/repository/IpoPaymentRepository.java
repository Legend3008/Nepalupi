package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.IpoPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpoPaymentRepository extends JpaRepository<IpoPayment, UUID> {

    List<IpoPayment> findByUserId(UUID userId);

    Page<IpoPayment> findByUserId(UUID userId, Pageable pageable);

    Optional<IpoPayment> findByUserIdAndIpoCode(UUID userId, String ipoCode);

    List<IpoPayment> findByIpoCode(String ipoCode);

    @Query("SELECT COUNT(i) FROM IpoPayment i WHERE i.ipoCode = :ipoCode AND i.status IN ('PENDING', 'VERIFIED')")
    long countActiveApplications(@Param("ipoCode") String ipoCode);

    @Query("SELECT i FROM IpoPayment i WHERE i.boid = :boid AND i.ipoCode = :ipoCode")
    Optional<IpoPayment> findByBoidAndIpoCode(@Param("boid") String boid, @Param("ipoCode") String ipoCode);
}
