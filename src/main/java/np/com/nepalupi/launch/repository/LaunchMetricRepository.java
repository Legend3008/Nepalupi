package np.com.nepalupi.launch.repository;

import np.com.nepalupi.launch.entity.LaunchMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LaunchMetricRepository extends JpaRepository<LaunchMetric, UUID> {

    Optional<LaunchMetric> findByMetricDate(LocalDate metricDate);

    List<LaunchMetric> findByMetricDateBetweenOrderByMetricDateAsc(LocalDate start, LocalDate end);

    @Query("SELECT m FROM LaunchMetric m ORDER BY m.metricDate DESC LIMIT 1")
    Optional<LaunchMetric> findLatest();

    @Query("SELECT m FROM LaunchMetric m ORDER BY m.metricDate DESC LIMIT :days")
    List<LaunchMetric> findRecentDays(@Param("days") int days);
}
