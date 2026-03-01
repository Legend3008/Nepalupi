package np.com.nepalupi.launch.repository;

import np.com.nepalupi.launch.entity.LaunchChecklistItem;
import np.com.nepalupi.launch.enums.ChecklistCategory;
import np.com.nepalupi.launch.enums.ChecklistItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LaunchChecklistItemRepository extends JpaRepository<LaunchChecklistItem, UUID> {

    List<LaunchChecklistItem> findByPhaseIdOrderByCategoryAsc(UUID phaseId);

    List<LaunchChecklistItem> findByPhaseIdAndCategory(UUID phaseId, ChecklistCategory category);

    List<LaunchChecklistItem> findByPhaseIdAndStatus(UUID phaseId, ChecklistItemStatus status);

    @Query("SELECT c FROM LaunchChecklistItem c WHERE c.phaseId = :phaseId AND c.isBlocking = true AND c.status <> 'COMPLETED'")
    List<LaunchChecklistItem> findIncompleteBlockingItems(@Param("phaseId") UUID phaseId);

    @Query("SELECT COUNT(c) FROM LaunchChecklistItem c WHERE c.phaseId = :phaseId AND c.isBlocking = true AND c.status <> 'COMPLETED'")
    long countIncompleteBlockingItems(@Param("phaseId") UUID phaseId);

    List<LaunchChecklistItem> findByOwner(String owner);
}
