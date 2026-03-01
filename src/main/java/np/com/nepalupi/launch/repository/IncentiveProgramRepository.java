package np.com.nepalupi.launch.repository;

import np.com.nepalupi.launch.entity.IncentiveProgram;
import np.com.nepalupi.launch.enums.IncentiveProgramType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncentiveProgramRepository extends JpaRepository<IncentiveProgram, UUID> {

    Optional<IncentiveProgram> findByProgramCode(String programCode);

    List<IncentiveProgram> findByProgramType(IncentiveProgramType programType);

    List<IncentiveProgram> findByIsActiveTrue();

    List<IncentiveProgram> findByPspId(String pspId);

    @Query("SELECT p FROM IncentiveProgram p WHERE p.isActive = true AND p.spentPaisa >= p.budgetPaisa")
    List<IncentiveProgram> findExhaustedPrograms();

    @Query("SELECT p FROM IncentiveProgram p WHERE p.isActive = true AND p.endDate < CURRENT_TIMESTAMP")
    List<IncentiveProgram> findExpiredButActivePrograms();
}
