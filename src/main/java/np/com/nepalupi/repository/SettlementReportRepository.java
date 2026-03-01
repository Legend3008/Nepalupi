package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.SettlementReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementReportRepository extends JpaRepository<SettlementReport, UUID> {

    Optional<SettlementReport> findBySettlementDate(LocalDate date);
}
