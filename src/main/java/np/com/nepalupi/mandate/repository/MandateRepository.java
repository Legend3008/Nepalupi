package np.com.nepalupi.mandate.repository;

import np.com.nepalupi.mandate.entity.Mandate;
import np.com.nepalupi.mandate.enums.MandateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MandateRepository extends JpaRepository<Mandate, UUID> {

    Optional<Mandate> findByMandateRef(String mandateRef);

    List<Mandate> findByPayerVpaAndStatusOrderByCreatedAtDesc(String payerVpa, MandateStatus status);

    List<Mandate> findByPayerVpaOrderByCreatedAtDesc(String payerVpa);

    List<Mandate> findByMerchantVpaOrderByCreatedAtDesc(String merchantVpa);

    @Query("SELECT m FROM Mandate m WHERE m.status = 'ACTIVE' AND m.nextDebitDate = :date")
    List<Mandate> findDueForExecution(@Param("date") LocalDate date);

    @Query("SELECT m FROM Mandate m WHERE m.status = 'ACTIVE' AND m.nextDebitDate = :date")
    List<Mandate> findDueForPreNotification(@Param("date") LocalDate date);

    @Query("SELECT m FROM Mandate m WHERE m.status = 'ACTIVE' AND m.endDate IS NOT NULL AND m.endDate < :date")
    List<Mandate> findExpiredMandates(@Param("date") LocalDate date);
}
