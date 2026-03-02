package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.dto.request.VpaResolveRequest;
import np.com.nepalupi.domain.dto.response.VpaDetails;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.repository.VpaRepository;
import np.com.nepalupi.service.vpa.VpaResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VPA API — resolve, list, and manage Virtual Payment Addresses.
 */
@RestController
@RequestMapping("/api/v1/vpa")
@RequiredArgsConstructor
@Tag(name = "VPA", description = "Virtual Payment Address resolution and management")
public class VpaController {

    private final VpaResolutionService vpaResolutionService;
    private final VpaRepository vpaRepository;

    /**
     * Resolve a VPA to its associated bank account details.
     */
    @PostMapping("/resolve")
    @Operation(summary = "Resolve VPA", description = "Resolve a VPA address to bank account details")
    public ResponseEntity<VpaDetails> resolve(@Valid @RequestBody VpaResolveRequest request) {
        VpaDetails details = vpaResolutionService.resolve(request.getVpa());
        return ResponseEntity.ok(details);
    }

    /**
     * List all active VPAs for a user.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "List user VPAs", description = "Get all active VPAs for a user ID")
    public ResponseEntity<List<Vpa>> listUserVpas(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        return ResponseEntity.ok(vpaRepository.findByUserIdAndIsActiveTrue(userId));
    }

    /**
     * Check if a VPA is available (not taken).
     */
    @GetMapping("/available/{vpa}")
    @Operation(summary = "Check VPA availability", description = "Check if a VPA address is available for registration")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @Parameter(description = "VPA address to check (e.g., newuser@nchl)") @PathVariable String vpa) {
        boolean exists = vpaRepository.existsByVpaAddress(vpa);
        return ResponseEntity.ok(Map.of(
                "vpa", vpa,
                "available", !exists
        ));
    }

    /**
     * Deactivate a VPA.
     */
    @DeleteMapping("/{vpa}")
    @Operation(summary = "Deactivate VPA", description = "Soft-delete / deactivate a VPA address")
    public ResponseEntity<Map<String, Object>> deactivateVpa(
            @Parameter(description = "VPA address to deactivate") @PathVariable String vpa) {
        Vpa vpaEntity = vpaRepository.findByVpaAddressAndIsActiveTrue(vpa)
                .orElseThrow(() -> new IllegalArgumentException("VPA not found or already inactive: " + vpa));

        vpaEntity.setIsActive(false);
        vpaRepository.save(vpaEntity);

        // Invalidate cache
        vpaResolutionService.invalidateCache(vpa);

        return ResponseEntity.ok(Map.of(
                "vpa", vpa,
                "status", "DEACTIVATED"
        ));
    }
}
