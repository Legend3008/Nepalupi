package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.PspHealthReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PspHealthReportRepository extends JpaRepository<PspHealthReport, UUID> {

    Optional<PspHealthReport> findByPspIdAndReportMonth(String pspId, LocalDate reportMonth);

    List<PspHealthReport> findByPspIdOrderByReportMonthDesc(String pspId);

    List<PspHealthReport> findByReportMonth(LocalDate reportMonth);
}
