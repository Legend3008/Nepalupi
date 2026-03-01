package np.com.nepalupi.mandate.repository;

import np.com.nepalupi.mandate.entity.MandateExecution;
import np.com.nepalupi.mandate.enums.MandateExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MandateExecutionRepository extends JpaRepository<MandateExecution, UUID> {

    List<MandateExecution> findByMandateIdOrderByScheduledDateDesc(UUID mandateId);

    List<MandateExecution> findByScheduledDateAndStatus(LocalDate date, MandateExecutionStatus status);

    List<MandateExecution> findByStatusIn(List<MandateExecutionStatus> statuses);
}
