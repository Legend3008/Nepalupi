package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.NrbDailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NrbDailyReportRepository extends JpaRepository<NrbDailyReport, UUID> {

    Optional<NrbDailyReport> findByReportDate(LocalDate reportDate);

    List<NrbDailyReport> findByReportDateBetweenOrderByReportDateDesc(LocalDate start, LocalDate end);

    List<NrbDailyReport> findBySubmittedFalseOrderByReportDateAsc();

    long countByReportDateBetween(LocalDate start, LocalDate end);
}
