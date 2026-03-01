package np.com.nepalupi.operations.repository;

import np.com.nepalupi.operations.entity.Runbook;
import np.com.nepalupi.operations.enums.RunbookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RunbookRepository extends JpaRepository<Runbook, UUID> {

    List<Runbook> findByCategory(RunbookCategory category);

    @Query("SELECT r FROM Runbook r WHERE LOWER(r.symptoms) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Runbook> searchByKeyword(String keyword);
}
