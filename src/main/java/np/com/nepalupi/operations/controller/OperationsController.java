package np.com.nepalupi.operations.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.operations.entity.*;
import np.com.nepalupi.operations.enums.IncidentStatus;
import np.com.nepalupi.operations.enums.RunbookCategory;
import np.com.nepalupi.operations.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Operations Controller — Module 11.
 * <p>
 * Incident Management:
 * POST /api/v1/ops/incidents                     → Create incident
 * POST /api/v1/ops/incidents/{id}/acknowledge     → Acknowledge
 * POST /api/v1/ops/incidents/{id}/status          → Update status
 * POST /api/v1/ops/incidents/{id}/escalate        → Escalate
 * POST /api/v1/ops/incidents/{id}/resolve         → Resolve
 * POST /api/v1/ops/incidents/{id}/note            → Add note
 * GET  /api/v1/ops/incidents/active               → Active incidents
 * GET  /api/v1/ops/incidents/{id}                 → Get incident
 * GET  /api/v1/ops/incidents/{id}/timeline        → Get timeline
 * <p>
 * Runbooks:
 * GET  /api/v1/ops/runbooks                       → All runbooks
 * GET  /api/v1/ops/runbooks/search                → Search by symptoms
 * POST /api/v1/ops/runbooks                       → Create runbook
 * <p>
 * On-Call:
 * GET  /api/v1/ops/oncall/current                 → Current on-call
 * POST /api/v1/ops/oncall                         → Create schedule
 * <p>
 * Postmortems:
 * POST /api/v1/ops/postmortems                    → Create postmortem
 * GET  /api/v1/ops/postmortems/drafts             → Get draft postmortems
 * <p>
 * Dashboard:
 * GET  /api/v1/ops/health                         → System health
 */
@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class OperationsController {

    private final IncidentService incidentService;
    private final RunbookService runbookService;
    private final OnCallService onCallService;
    private final PostmortemService postmortemService;
    private final MonitoringDashboardService monitoringService;

    // ── Incidents ──

    @PostMapping("/incidents")
    public ResponseEntity<Incident> createIncident(
            @RequestParam int severity,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String affectedService,
            @RequestParam(required = false) String affectedBankCode) {
        return ResponseEntity.ok(incidentService.createIncident(
                severity, title, description, affectedService, affectedBankCode));
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public ResponseEntity<Incident> acknowledgeIncident(
            @PathVariable UUID id,
            @RequestParam String engineerName) {
        return ResponseEntity.ok(incidentService.acknowledge(id, engineerName));
    }

    @PostMapping("/incidents/{id}/status")
    public ResponseEntity<Incident> updateStatus(
            @PathVariable UUID id,
            @RequestParam IncidentStatus status,
            @RequestParam(required = false) String note,
            @RequestParam String author) {
        return ResponseEntity.ok(incidentService.updateStatus(id, status, note, author));
    }

    @PostMapping("/incidents/{id}/escalate")
    public ResponseEntity<Incident> escalateIncident(
            @PathVariable UUID id,
            @RequestParam String escalatedTo,
            @RequestParam String reason,
            @RequestParam String author) {
        return ResponseEntity.ok(incidentService.escalate(id, escalatedTo, reason, author));
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<Incident> resolveIncident(
            @PathVariable UUID id,
            @RequestParam String rootCause,
            @RequestParam String resolutionSummary,
            @RequestParam String author) {
        return ResponseEntity.ok(incidentService.resolve(id, rootCause, resolutionSummary, author));
    }

    @PostMapping("/incidents/{id}/note")
    public ResponseEntity<IncidentTimeline> addNote(
            @PathVariable UUID id,
            @RequestParam String message,
            @RequestParam String author) {
        return ResponseEntity.ok(incidentService.addNote(id, message, author));
    }

    @GetMapping("/incidents/active")
    public ResponseEntity<List<Incident>> getActiveIncidents() {
        return ResponseEntity.ok(incidentService.getActiveIncidents());
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<Incident> getIncident(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getIncident(id));
    }

    @GetMapping("/incidents/{id}/timeline")
    public ResponseEntity<List<IncidentTimeline>> getTimeline(@PathVariable UUID id) {
        return ResponseEntity.ok(incidentService.getTimeline(id));
    }

    // ── Runbooks ──

    @GetMapping("/runbooks")
    public ResponseEntity<List<Runbook>> getAllRunbooks() {
        return ResponseEntity.ok(runbookService.getAll());
    }

    @GetMapping("/runbooks/search")
    public ResponseEntity<List<Runbook>> searchRunbooks(@RequestParam String keyword) {
        return ResponseEntity.ok(runbookService.searchBySymptoms(keyword));
    }

    @GetMapping("/runbooks/category/{category}")
    public ResponseEntity<List<Runbook>> getByCategory(@PathVariable RunbookCategory category) {
        return ResponseEntity.ok(runbookService.getByCategory(category));
    }

    @PostMapping("/runbooks")
    public ResponseEntity<Runbook> createRunbook(@RequestBody Runbook runbook) {
        return ResponseEntity.ok(runbookService.save(runbook));
    }

    // ── On-Call ──

    @GetMapping("/oncall/current")
    public ResponseEntity<List<OnCallSchedule>> getCurrentOnCall() {
        return ResponseEntity.ok(onCallService.getCurrentSchedule());
    }

    @PostMapping("/oncall")
    public ResponseEntity<OnCallSchedule> createSchedule(@RequestBody OnCallSchedule schedule) {
        return ResponseEntity.ok(onCallService.createSchedule(schedule));
    }

    // ── Postmortems ──

    @PostMapping("/postmortems")
    public ResponseEntity<Postmortem> createPostmortem(@RequestBody Postmortem postmortem) {
        return ResponseEntity.ok(postmortemService.create(postmortem));
    }

    @GetMapping("/postmortems/incident/{incidentId}")
    public ResponseEntity<Postmortem> getPostmortem(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(postmortemService.getByIncident(incidentId));
    }

    @GetMapping("/postmortems/drafts")
    public ResponseEntity<List<Postmortem>> getDraftPostmortems() {
        return ResponseEntity.ok(postmortemService.getDrafts());
    }

    @PostMapping("/postmortems/{id}/complete")
    public ResponseEntity<Postmortem> completePostmortem(@PathVariable UUID id) {
        return ResponseEntity.ok(postmortemService.complete(id));
    }

    // ── Dashboard ──

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(monitoringService.getSystemHealth());
    }
}
