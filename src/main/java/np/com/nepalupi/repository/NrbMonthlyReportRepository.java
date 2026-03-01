package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.NrbMonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NrbMonthlyReportRepository extends JpaRepository<NrbMonthlyReport, UUID> {

    Optional<NrbMonthlyReport> findByReportMonth(LocalDate reportMonth);
}
