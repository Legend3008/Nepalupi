package np.com.nepalupi.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.operations.entity.OnCallSchedule;
import np.com.nepalupi.operations.repository.OnCallScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * On-Call Schedule Service — manages rotation schedules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnCallService {

    private final OnCallScheduleRepository onCallRepository;

    /**
     * Get the current primary on-call engineer.
     */
    public OnCallSchedule getCurrentPrimary() {
        return onCallRepository.findCurrentOnCall(LocalDate.now())
                .stream()
                .filter(s -> "PRIMARY".equals(s.getRole()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the current secondary on-call engineer.
     */
    public OnCallSchedule getCurrentSecondary() {
        return onCallRepository.findCurrentOnCall(LocalDate.now())
                .stream()
                .filter(s -> "SECONDARY".equals(s.getRole()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all current on-call engineers.
     */
    public List<OnCallSchedule> getCurrentSchedule() {
        return onCallRepository.findCurrentOnCall(LocalDate.now());
    }

    /**
     * Create a new on-call schedule entry.
     */
    @Transactional
    public OnCallSchedule createSchedule(OnCallSchedule schedule) {
        return onCallRepository.save(schedule);
    }

    /**
     * Get all active schedules.
     */
    public List<OnCallSchedule> getAllSchedules() {
        return onCallRepository.findByIsActiveTrueOrderByWeekStartDesc();
    }
}
