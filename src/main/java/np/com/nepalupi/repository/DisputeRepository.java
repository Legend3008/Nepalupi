package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByTransactionId(UUID transactionId);

    List<Dispute> findByStatus(String status);

    Optional<Dispute> findByCaseRef(String caseRef);

    List<Dispute> findByDisputeType(String disputeType);

    List<Dispute> findByRaisedByVpaOrderByCreatedAtDesc(String raisedByVpa);

    @Query("SELECT d FROM Dispute d WHERE d.slaDeadline IS NOT NULL AND d.slaDeadline < :now " +
            "AND d.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Dispute> findSlaBreaches(@Param("now") Instant now);

    @Query("SELECT d FROM Dispute d WHERE d.status NOT IN ('RESOLVED', 'CLOSED') " +
            "AND d.escalationLevel < 1 AND d.createdAt < :threshold")
    List<Dispute> findStaleDisputes(@Param("threshold") Instant threshold);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.createdAt >= :start AND d.createdAt < :end")
    long countByDateRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(d) FROM Dispute d WHERE d.status = 'RESOLVED' AND d.autoResolved = true " +
            "AND d.resolvedAt >= :start AND d.resolvedAt < :end")
    long countAutoResolvedByDateRange(@Param("start") Instant start, @Param("end") Instant end);
}
