package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.NrbQuarterlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NrbQuarterlyReportRepository extends JpaRepository<NrbQuarterlyReport, UUID> {

    Optional<NrbQuarterlyReport> findByReportQuarter(String reportQuarter);
}
