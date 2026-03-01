package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.DailyTransactionStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyTransactionStatsRepository extends JpaRepository<DailyTransactionStats, UUID> {

    Optional<DailyTransactionStats> findByUserIdAndStatsDate(UUID userId, LocalDate statsDate);
}
