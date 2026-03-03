package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.Grievance;
import np.com.nepalupi.service.dispute.GrievanceRedressalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Grievance redressal API as per NRB customer protection guidelines.
 */
@RestController
@RequestMapping("/api/v1/grievances")
@RequiredArgsConstructor
public class GrievanceController {

    private final GrievanceRedressalService grievanceService;

    @PostMapping
    public ResponseEntity<Grievance> fileGrievance(@RequestBody Map<String, String> request) {
        UUID userId = UUID.fromString(request.get("userId"));
        Grievance g = grievanceService.fileGrievance(
                userId,
                request.get("category"),
                request.get("subject"),
                request.get("description")
        );
        return ResponseEntity.ok(g);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Grievance>> getUserGrievances(@PathVariable UUID userId) {
        return ResponseEntity.ok(grievanceService.getUserGrievances(userId));
    }

    @GetMapping("/ticket/{ticketNumber}")
    public ResponseEntity<Grievance> getByTicket(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(grievanceService.getByTicketNumber(ticketNumber));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Grievance>> getByStatus(@PathVariable String status, Pageable pageable) {
        return ResponseEntity.ok(grievanceService.getByStatus(status, pageable));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<Grievance> assign(@PathVariable UUID id, @RequestParam String assignedTo) {
        return ResponseEntity.ok(grievanceService.assignGrievance(id, assignedTo));
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<Grievance> escalate(@PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(grievanceService.escalate(id, reason));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Grievance> resolve(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(grievanceService.resolve(id, body.get("resolution")));
    }

    @PutMapping("/{id}/reopen")
    public ResponseEntity<Grievance> reopen(@PathVariable UUID id, @RequestParam String reason) {
        return ResponseEntity.ok(grievanceService.reopen(id, reason));
    }
}
