package np.com.nepalupi.operations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.operations.entity.Incident;
import np.com.nepalupi.operations.entity.IncidentTimeline;
import np.com.nepalupi.operations.entity.OnCallSchedule;
import np.com.nepalupi.operations.enums.IncidentStatus;
import np.com.nepalupi.operations.enums.TimelineEntryType;
import np.com.nepalupi.operations.repository.IncidentRepository;
import np.com.nepalupi.operations.repository.IncidentTimelineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Incident Service — core incident lifecycle management.
 * <p>
 * Follows Google SRE incident response model adapted for a UPI switch:
 * DETECT → ACKNOWLEDGE → INVESTIGATE → MITIGATE → RESOLVE → POSTMORTEM
 * <p>
 * - Auto-pages on-call engineer
 * - Auto-escalates if SLA breached
 * - SEV1/SEV2 auto-notifies NRB (Nepal Rastra Bank)
 * - All actions logged to incident timeline
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository timelineRepository;
    private final OnCallService onCallService;

    /**
     * Create a new incident.
     */
    @Transactional
    public Incident createIncident(int severity, String title, String description,
                                    String affectedService, String affectedBankCode) {
        String incidentNumber = generateIncidentNumber();

        Incident incident = Incident.builder()
                .incidentNumber(incidentNumber)
                .severity(severity)
                .title(title)
                .description(description)
                .status(IncidentStatus.DETECTED)
                .detectedAt(Instant.now())
                .affectedService(affectedService)
                .affectedBankCode(affectedBankCode)
                .build();

        // Auto-assign on-call engineer
        OnCallSchedule onCall = onCallService.getCurrentPrimary();
        if (onCall != null) {
            incident.setOnCallEngineer(onCall.getEngineerName());
        }

        incident = incidentRepository.save(incident);

        addTimelineEntry(incident.getId(), TimelineEntryType.STATUS_CHANGE,
                "Incident detected: " + title, "SYSTEM");

        // Auto-page on-call
        if (onCall != null) {
            pageEngineer(onCall, incident);
            addTimelineEntry(incident.getId(), TimelineEntryType.NOTIFICATION,
                    "On-call engineer paged: " + onCall.getEngineerName(), "SYSTEM");
        }

        // SEV1/SEV2: auto-notify NRB
        if (severity <= 2) {
            notifyNrb(incident);
        }

        log.info("Incident created: {} SEV{} - {}", incidentNumber, severity, title);
        return incident;
    }

    /**
     * Acknowledge an incident (engineer responding).
     */
    @Transactional
    public Incident acknowledge(UUID incidentId, String engineerName) {
        Incident incident = getIncident(incidentId);
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(Instant.now());
        incident.setOnCallEngineer(engineerName);
        incident = incidentRepository.save(incident);

        addTimelineEntry(incidentId, TimelineEntryType.STATUS_CHANGE,
                "Incident acknowledged", engineerName);
        return incident;
    }

    /**
     * Update investigation status.
     */
    @Transactional
    public Incident updateStatus(UUID incidentId, IncidentStatus newStatus,
                                  String note, String author) {
        Incident incident = getIncident(incidentId);
        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(newStatus);

        if (newStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(Instant.now());
            incident.setDurationMinutes(
                    (int) Duration.between(incident.getDetectedAt(), Instant.now()).toMinutes());
        }

        incident = incidentRepository.save(incident);

        addTimelineEntry(incidentId, TimelineEntryType.STATUS_CHANGE,
                oldStatus + " → " + newStatus + (note != null ? ": " + note : ""), author);
        return incident;
    }

    /**
     * Escalate an incident.
     */
    @Transactional
    public Incident escalate(UUID incidentId, String escalatedTo, String reason, String author) {
        Incident incident = getIncident(incidentId);
        incident.setEscalationLevel(incident.getEscalationLevel() + 1);
        incident.setEscalatedTo(escalatedTo);
        incident = incidentRepository.save(incident);

        addTimelineEntry(incidentId, TimelineEntryType.ESCALATION,
                "Escalated to " + escalatedTo + ": " + reason, author);
        return incident;
    }

    /**
     * Resolve with root cause.
     */
    @Transactional
    public Incident resolve(UUID incidentId, String rootCause, String resolutionSummary,
                             String author) {
        Incident incident = getIncident(incidentId);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        incident.setRootCause(rootCause);
        incident.setResolutionSummary(resolutionSummary);
        incident.setDurationMinutes(
                (int) Duration.between(incident.getDetectedAt(), Instant.now()).toMinutes());
        incident = incidentRepository.save(incident);

        addTimelineEntry(incidentId, TimelineEntryType.STATUS_CHANGE,
                "Resolved: " + resolutionSummary, author);

        // SEV1/SEV2: update status page, notify PSPs
        if (incident.getSeverity() <= 2) {
            addTimelineEntry(incidentId, TimelineEntryType.NOTIFICATION,
                    "NRB and PSPs notified of resolution", "SYSTEM");
        }

        return incident;
    }

    /**
     * Add a timeline note.
     */
    @Transactional
    public IncidentTimeline addNote(UUID incidentId, String message, String author) {
        return addTimelineEntry(incidentId, TimelineEntryType.NOTE, message, author);
    }

    /**
     * Get incident with full timeline.
     */
    public Incident getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
    }

    /**
     * Get timeline for an incident.
     */
    public List<IncidentTimeline> getTimeline(UUID incidentId) {
        return timelineRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }

    /**
     * Get all active incidents.
     */
    public List<Incident> getActiveIncidents() {
        return incidentRepository.findActiveIncidents();
    }

    // ── Helpers ──

    private IncidentTimeline addTimelineEntry(UUID incidentId, TimelineEntryType type,
                                               String message, String author) {
        IncidentTimeline entry = IncidentTimeline.builder()
                .incidentId(incidentId)
                .entryType(type)
                .message(message)
                .author(author)
                .build();
        return timelineRepository.save(entry);
    }

    private String generateIncidentNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = incidentRepository.count() + 1;
        return "INC-" + date + "-" + String.format("%03d", count);
    }

    private void pageEngineer(OnCallSchedule onCall, Incident incident) {
        // In production: PagerDuty / Opsgenie / SMS alert
        log.warn("PAGING on-call {}: {} - SEV{} - {}",
                onCall.getEngineerName(), incident.getIncidentNumber(),
                incident.getSeverity(), incident.getTitle());
    }

    private void notifyNrb(Incident incident) {
        // In production: send email/API call to NRB
        incident.setNrbNotified(true);
        incident.setNrbNotifiedAt(Instant.now());
        log.warn("NRB NOTIFIED of SEV{} incident: {} - {}",
                incident.getSeverity(), incident.getIncidentNumber(), incident.getTitle());
    }
}
