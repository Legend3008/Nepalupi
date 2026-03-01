package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.Transaction;
import np.com.nepalupi.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByUpiTxnId(String upiTxnId);

    Optional<Transaction> findByRrn(String rrn);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.expiresAt < :now")
    List<Transaction> findExpiredTransactions(
            @Param("status") TransactionStatus status,
            @Param("now") Instant now);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.status = 'COMPLETED'
            AND t.completedAt >= :startOfDay
            AND t.completedAt < :startOfNextDay
            """)
    List<Transaction> findCompletedByDate(
            @Param("startOfDay") Instant startOfDay,
            @Param("startOfNextDay") Instant startOfNextDay);

    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.payerVpa IN " +
            "(SELECT v.vpaAddress FROM Vpa v WHERE v.userId = :userId) " +
            "AND t.initiatedAt >= :since AND t.status = 'COMPLETED'")
    Double getAverageAmount(@Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.payerVpa IN " +
            "(SELECT v.vpaAddress FROM Vpa v WHERE v.userId = :userId) " +
            "AND t.initiatedAt >= :since")
    int countTransactionsSince(@Param("userId") UUID userId, @Param("since") Instant since);

    List<Transaction> findByPayerVpaOrderByInitiatedAtDesc(String payerVpa);

    List<Transaction> findByPayeeVpaOrderByInitiatedAtDesc(String payeeVpa);
}
