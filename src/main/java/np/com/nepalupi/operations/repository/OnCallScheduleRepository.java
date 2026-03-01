package np.com.nepalupi.operations.repository;

import np.com.nepalupi.operations.entity.OnCallSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OnCallScheduleRepository extends JpaRepository<OnCallSchedule, UUID> {

    @Query("SELECT o FROM OnCallSchedule o WHERE o.isActive = true AND :date BETWEEN o.weekStart AND o.weekEnd ORDER BY o.role")
    List<OnCallSchedule> findCurrentOnCall(@Param("date") LocalDate date);

    List<OnCallSchedule> findByIsActiveTrueOrderByWeekStartDesc();
}
