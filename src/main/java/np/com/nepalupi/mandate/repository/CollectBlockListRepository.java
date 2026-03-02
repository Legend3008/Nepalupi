package np.com.nepalupi.mandate.repository;

import np.com.nepalupi.mandate.entity.CollectBlockList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CollectBlockListRepository extends JpaRepository<CollectBlockList, UUID> {
    boolean existsByPayerVpaAndBlockedVpa(String payerVpa, String blockedVpa);
    List<CollectBlockList> findByPayerVpa(String payerVpa);
    void deleteByPayerVpaAndBlockedVpa(String payerVpa, String blockedVpa);
}
