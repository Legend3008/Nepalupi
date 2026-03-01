package np.com.nepalupi.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.operations.entity.Postmortem;
import np.com.nepalupi.operations.entity.Incident;
import np.com.nepalupi.operations.enums.IncidentStatus;
import np.com.nepalupi.operations.repository.IncidentRepository;
import np.com.nepalupi.operations.repository.PostmortemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Postmortem Service — blameless post-incident reviews.
 * <p>
 * Every SEV1/SEV2 incident requires a postmortem within 48 hours.
 * Follows Google SRE blameless postmortem model:
 * - What happened (timeline)
 * - Root cause analysis
 * - Contributing factors
 * - Action items with owners and due dates
 * - Shared with entire engineering team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostmortemService {

    private final PostmortemRepository postmortemRepository;
    private final IncidentRepository incidentRepository;

    /**
     * Create a postmortem for an incident.
     */
    @Transactional
    public Postmortem create(Postmortem postmortem) {
        Incident incident = incidentRepository.findById(postmortem.getIncidentId())
                .orElseThrow(() -> new IllegalArgumentException("Incident not found"));

        if (incident.getStatus() != IncidentStatus.RESOLVED &&
            incident.getStatus() != IncidentStatus.POSTMORTEM_PENDING) {
            throw new IllegalStateException("Incident must be resolved before postmortem");
        }

        incident.setStatus(IncidentStatus.POSTMORTEM_PENDING);
        incidentRepository.save(incident);

        postmortem = postmortemRepository.save(postmortem);
        log.info("Postmortem created for incident={}", incident.getIncidentNumber());
        return postmortem;
    }

    /**
     * Get postmortem for an incident.
     */
    public Postmortem getByIncident(UUID incidentId) {
        return postmortemRepository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("No postmortem found for incident"));
    }

    /**
     * Get all draft postmortems (needing review).
     */
    public List<Postmortem> getDrafts() {
        return postmortemRepository.findByStatusOrderByCreatedAtDesc("DRAFT");
    }

    /**
     * Complete a postmortem review.
     */
    @Transactional
    public Postmortem complete(UUID postmortemId) {
        Postmortem pm = postmortemRepository.findById(postmortemId)
                .orElseThrow(() -> new IllegalArgumentException("Postmortem not found"));
        pm.setStatus("COMPLETED");
        return postmortemRepository.save(pm);
    }
}
