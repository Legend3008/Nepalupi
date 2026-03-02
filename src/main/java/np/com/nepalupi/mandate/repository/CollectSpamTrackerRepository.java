package np.com.nepalupi.mandate.repository;

import np.com.nepalupi.mandate.entity.CollectSpamTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectSpamTrackerRepository extends JpaRepository<CollectSpamTracker, UUID> {

    Optional<CollectSpamTracker> findByRequestorVpaAndTargetPayerVpaAndRequestDate(
            String requestorVpa, String targetPayerVpa, LocalDate requestDate);

    @Query("SELECT COALESCE(SUM(s.requestCount), 0) FROM CollectSpamTracker s " +
           "WHERE s.targetPayerVpa = :payerVpa AND s.requestDate = :date")
    long countTotalRequestsToPayerToday(@Param("payerVpa") String payerVpa,
                                         @Param("date") LocalDate date);
}
