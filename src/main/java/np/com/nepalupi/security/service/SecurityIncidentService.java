package np.com.nepalupi.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.security.entity.SecurityIncident;
import np.com.nepalupi.security.enums.SecurityIncidentStatus;
import np.com.nepalupi.security.enums.SecurityIncidentType;
import np.com.nepalupi.security.repository.SecurityIncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * Security incident response — manages the lifecycle of security breaches and attacks.
 * Three simultaneous tracks: Contain, Investigate, Communicate.
 * NRB must be notified immediately for any data breach or CRITICAL incident.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityIncidentService {

    private final SecurityIncidentRepository incidentRepository;

    @Transactional
    public SecurityIncident reportIncident(SecurityIncidentType type, String severity,
                                            String title, String description,
                                            String attackVector, String affectedSystems) {
        long count = incidentRepository.count();
        String reference = String.format("SEC-INC-%d-%03d", Year.now().getValue(), count + 1);

        SecurityIncident incident = SecurityIncident.builder()
                .incidentReference(reference)
                .incidentType(type)
                .severity(severity)
                .title(title)
                .description(description)
                .attackVector(attackVector)
                .affectedSystems(affectedSystems)
                .status(SecurityIncidentStatus.DETECTED)
                .detectedAt(Instant.now())
                .build();

        incident = incidentRepository.save(incident);
        log.error("SECURITY INCIDENT DETECTED: ref={}, type={}, severity={}, title={}",
                reference, type, severity, title);

        // Auto-notify NRB for CRITICAL severity
        if ("CRITICAL".equals(severity)) {
            notifyNrb(incident.getId());
        }

        return incident;
    }

    @Transactional
    public SecurityIncident containIncident(UUID incidentId, String incidentCommander) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setStatus(SecurityIncidentStatus.CONTAINED);
        incident.setContainedAt(Instant.now());
        incident.setIncidentCommander(incidentCommander);

        log.warn("Security incident CONTAINED: ref={}, commander={}",
                incident.getIncidentReference(), incidentCommander);
        return incidentRepository.save(incident);
    }

    @Transactional
    public SecurityIncident startInvestigation(UUID incidentId) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setStatus(SecurityIncidentStatus.INVESTIGATING);
        return incidentRepository.save(incident);
    }

    @Transactional
    public SecurityIncident markDataCompromised(UUID incidentId, String dataDescription, int usersAffected) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setDataCompromised(true);
        incident.setDataCompromisedDescription(dataDescription);
        incident.setUsersAffectedCount(usersAffected);

        log.error("DATA BREACH CONFIRMED: ref={}, usersAffected={}, data={}",
                incident.getIncidentReference(), usersAffected, dataDescription);

        // NRB notification is mandatory for data breaches
        if (!incident.getNrbNotified()) {
            notifyNrb(incidentId);
        }

        return incidentRepository.save(incident);
    }

    @Transactional
    public SecurityIncident eradicate(UUID incidentId) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setStatus(SecurityIncidentStatus.ERADICATED);
        incident.setEradicatedAt(Instant.now());
        log.info("Security incident eradicated: ref={}", incident.getIncidentReference());
        return incidentRepository.save(incident);
    }

    @Transactional
    public SecurityIncident recover(UUID incidentId) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setStatus(SecurityIncidentStatus.RECOVERED);
        incident.setRecoveredAt(Instant.now());
        log.info("Security incident recovered: ref={}", incident.getIncidentReference());
        return incidentRepository.save(incident);
    }

    @Transactional
    public SecurityIncident closeIncident(UUID incidentId, String rootCause, String lessonsLearned) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setStatus(SecurityIncidentStatus.CLOSED);
        incident.setClosedAt(Instant.now());
        incident.setRootCause(rootCause);
        incident.setLessonsLearned(lessonsLearned);

        log.info("Security incident CLOSED: ref={}, rootCause={}", incident.getIncidentReference(), rootCause);
        return incidentRepository.save(incident);
    }

    @Transactional
    public SecurityIncident notifyNrb(UUID incidentId) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setNrbNotified(true);
        incident.setNrbNotifiedAt(Instant.now());
        log.info("NRB notified of security incident: ref={}", incident.getIncidentReference());
        // In production: send formal notification to NRB via secure channel
        return incidentRepository.save(incident);
    }

    @Transactional
    public SecurityIncident notifyAffectedUsers(UUID incidentId) {
        SecurityIncident incident = getOrThrow(incidentId);
        incident.setUsersNotified(true);
        incident.setUsersNotifiedAt(Instant.now());
        log.info("Affected users notified: ref={}, count={}", incident.getIncidentReference(), incident.getUsersAffectedCount());
        // In production: trigger user notification via SMS/push
        return incidentRepository.save(incident);
    }

    public List<SecurityIncident> getActiveIncidents() {
        return incidentRepository.findActiveIncidents();
    }

    public List<SecurityIncident> getBreachesWithUnnotifiedUsers() {
        return incidentRepository.findBreachesWithUnnotifiedUsers();
    }

    public SecurityIncident getByReference(String reference) {
        return incidentRepository.findByIncidentReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + reference));
    }

    private SecurityIncident getOrThrow(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Security incident not found: " + id));
    }
}
