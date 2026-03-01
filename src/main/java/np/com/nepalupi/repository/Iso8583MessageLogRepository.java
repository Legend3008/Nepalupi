package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Iso8583MessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface Iso8583MessageLogRepository extends JpaRepository<Iso8583MessageLog, Long> {

    List<Iso8583MessageLog> findByRrnOrderByCreatedAtAsc(String rrn);

    List<Iso8583MessageLog> findByStanOrderByCreatedAtAsc(String stan);

    List<Iso8583MessageLog> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);

    @Query("SELECT m FROM Iso8583MessageLog m WHERE m.rrn = :rrn AND m.direction = 'INBOUND' AND m.mti = '0210'")
    Optional<Iso8583MessageLog> findResponseByRrn(@Param("rrn") String rrn);

    @Query("SELECT m FROM Iso8583MessageLog m WHERE m.createdAt >= :start AND m.createdAt < :end ORDER BY m.createdAt")
    List<Iso8583MessageLog> findByDateRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(m) FROM Iso8583MessageLog m WHERE m.direction = 'OUTBOUND' AND m.mti = '0200' AND m.createdAt >= :since")
    long countOutboundFinancialSince(@Param("since") Instant since);
}
