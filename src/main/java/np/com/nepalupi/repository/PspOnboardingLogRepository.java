package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.PspOnboardingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PspOnboardingLogRepository extends JpaRepository<PspOnboardingLog, Long> {

    List<PspOnboardingLog> findByPspIdOrderByCreatedAtAsc(String pspId);
}
