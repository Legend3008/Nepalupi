package np.com.nepalupi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.dto.request.VpaResolveRequest;
import np.com.nepalupi.domain.dto.response.VpaDetails;
import np.com.nepalupi.service.vpa.VpaResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * VPA API — resolve Virtual Payment Addresses.
 */
@RestController
@RequestMapping("/api/v1/vpa")
@RequiredArgsConstructor
public class VpaController {

    private final VpaResolutionService vpaResolutionService;

    /**
     * Resolve a VPA to its associated bank account details.
     * <p>
     * POST /api/v1/vpa/resolve
     */
    @PostMapping("/resolve")
    public ResponseEntity<VpaDetails> resolve(@Valid @RequestBody VpaResolveRequest request) {
        VpaDetails details = vpaResolutionService.resolve(request.getVpa());
        return ResponseEntity.ok(details);
    }
}
