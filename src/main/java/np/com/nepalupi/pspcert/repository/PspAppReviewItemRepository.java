package np.com.nepalupi.pspcert.repository;

import np.com.nepalupi.pspcert.entity.PspAppReviewItem;
import np.com.nepalupi.pspcert.enums.ReviewResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PspAppReviewItemRepository extends JpaRepository<PspAppReviewItem, UUID> {

    List<PspAppReviewItem> findByCertificationIdOrderByCreatedAt(UUID certificationId);

    List<PspAppReviewItem> findByCertificationIdAndReviewStageOrderByCreatedAt(UUID certificationId, String reviewStage);

    List<PspAppReviewItem> findByCertificationIdAndResultOrderByCreatedAt(UUID certificationId, ReviewResult result);

    long countByCertificationIdAndIsMandatoryTrueAndResult(UUID certificationId, ReviewResult result);
}
