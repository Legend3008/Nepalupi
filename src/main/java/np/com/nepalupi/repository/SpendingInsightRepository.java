package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.SpendingInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpendingInsightRepository extends JpaRepository<SpendingInsight, UUID> {

    List<SpendingInsight> findByUserIdAndPeriodTypeAndPeriodValue(UUID userId, String periodType, String periodValue);

    List<SpendingInsight> findByUserIdAndPeriodTypeOrderByTotalSpentPaisaDesc(UUID userId, String periodType);

    @Query("SELECT s FROM SpendingInsight s WHERE s.userId = :userId AND s.periodType = :periodType " +
           "ORDER BY s.periodValue DESC")
    List<SpendingInsight> findTrendByUserAndPeriodType(UUID userId, String periodType);

    void deleteByUserIdAndPeriodTypeAndPeriodValue(UUID userId, String periodType, String periodValue);
}
