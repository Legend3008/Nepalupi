package np.com.nepalupi.repository;

import np.com.nepalupi.domain.entity.CategoryTransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CategoryTransactionLimitRepository extends JpaRepository<CategoryTransactionLimit, UUID> {

    @Query("SELECT c FROM CategoryTransactionLimit c WHERE c.category = :category AND c.isActive = true AND c.effectiveFrom <= :date ORDER BY c.effectiveFrom DESC LIMIT 1")
    Optional<CategoryTransactionLimit> findActiveByCategory(@Param("category") String category, @Param("date") LocalDate date);
}
