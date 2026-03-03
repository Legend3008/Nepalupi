package np.com.nepalupi.service.dispute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Grievance;
import np.com.nepalupi.repository.GrievanceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Grievance Redressal Service as per NRB guidelines.
 * Supports filing, tracking, escalation, and resolution of customer grievances.
 * SLA: Level 1 = 48hrs, Level 2 = 7 days, Level 3 (NRB/Ombudsman) = 30 days.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceRedressalService {

    private final GrievanceRepository grievanceRepository;
    private static final AtomicLong ticketCounter = new AtomicLong(100000);

    /**
     * File a new grievance.
     */
    public Grievance fileGrievance(UUID userId, String category, String subject, String description) {
        String ticketNumber = "GRV-" + ticketCounter.incrementAndGet();

        Grievance grievance = Grievance.builder()
                .userId(userId)
                .category(category)
                .subject(subject)
                .description(description)
                .status("OPEN")
                .priority(determinePriority(category))
                .escalationLevel(0)
                .ticketNumber(ticketNumber)
                .slaDeadline(Instant.now().plus(48, ChronoUnit.HOURS)) // Level 0: 48hrs
                .build();

        grievanceRepository.save(grievance);
        log.info("Grievance filed: ticket={}, userId={}, category={}", ticketNumber, userId, category);
        return grievance;
    }

    /**
     * Assign grievance to a support agent.
     */
    public Grievance assignGrievance(UUID grievanceId, String assignedTo) {
        Grievance g = findById(grievanceId);
        g.setAssignedTo(assignedTo);
        g.setStatus("IN_PROGRESS");
        g.setUpdatedAt(Instant.now());
        grievanceRepository.save(g);
        log.info("Grievance assigned: id={}, agent={}", grievanceId, assignedTo);
        return g;
    }

    /**
     * Escalate grievance to next level.
     * Level 0 → Level 1 (Senior Agent, 7 days SLA)
     * Level 1 → Level 2 (Grievance Officer, 15 days SLA)
     * Level 2 → Level 3 (NRB/Ombudsman, 30 days SLA)
     */
    public Grievance escalate(UUID grievanceId, String reason) {
        Grievance g = findById(grievanceId);
        int newLevel = g.getEscalationLevel() + 1;

        if (newLevel > 3) {
            throw new IllegalStateException("Maximum escalation level (3) reached");
        }

        g.setEscalationLevel(newLevel);
        g.setStatus("ESCALATED");
        g.setAssignedTo(null); // Reassignment needed

        long slaDays = switch (newLevel) {
            case 1 -> 7;
            case 2 -> 15;
            case 3 -> 30;
            default -> 7;
        };
        g.setSlaDeadline(Instant.now().plus(slaDays, ChronoUnit.DAYS));
        g.setUpdatedAt(Instant.now());

        grievanceRepository.save(g);
        log.info("Grievance escalated: id={}, newLevel={}, reason={}", grievanceId, newLevel, reason);
        return g;
    }

    /**
     * Resolve the grievance.
     */
    public Grievance resolve(UUID grievanceId, String resolution) {
        Grievance g = findById(grievanceId);
        g.setStatus("RESOLVED");
        g.setResolution(resolution);
        g.setResolvedAt(Instant.now());
        g.setUpdatedAt(Instant.now());
        grievanceRepository.save(g);
        log.info("Grievance resolved: id={}, ticket={}", grievanceId, g.getTicketNumber());
        return g;
    }

    /**
     * Reopen a resolved grievance.
     */
    public Grievance reopen(UUID grievanceId, String reason) {
        Grievance g = findById(grievanceId);
        if (!"RESOLVED".equals(g.getStatus())) {
            throw new IllegalStateException("Can only reopen RESOLVED grievances");
        }
        g.setStatus("REOPENED");
        g.setResolution(null);
        g.setResolvedAt(null);
        g.setSlaDeadline(Instant.now().plus(48, ChronoUnit.HOURS));
        g.setUpdatedAt(Instant.now());
        grievanceRepository.save(g);
        log.info("Grievance reopened: id={}, reason={}", grievanceId, reason);
        return g;
    }

    public List<Grievance> getUserGrievances(UUID userId) {
        return grievanceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Grievance getByTicketNumber(String ticketNumber) {
        return grievanceRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + ticketNumber));
    }

    public Page<Grievance> getByStatus(String status, Pageable pageable) {
        return grievanceRepository.findByStatus(status, pageable);
    }

    private String determinePriority(String category) {
        return switch (category.toUpperCase()) {
            case "FRAUD", "UNAUTHORIZED_TRANSACTION", "ACCOUNT_BLOCKED" -> "HIGH";
            case "FAILED_TRANSACTION", "WRONG_DEBIT", "REFUND_PENDING" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private Grievance findById(UUID id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Grievance not found: " + id));
    }
}
