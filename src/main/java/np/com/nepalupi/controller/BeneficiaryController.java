package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.Beneficiary;
import np.com.nepalupi.repository.BeneficiaryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Beneficiary management — save, list, favorite, and manage payees.
 */
@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiaries", description = "Save and manage payment beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryRepository beneficiaryRepository;

    @PostMapping
    @Operation(summary = "Save beneficiary", description = "Save a payee for quick future payments")
    public ResponseEntity<Beneficiary> save(@RequestBody Beneficiary request) {
        // Check if already exists
        return beneficiaryRepository.findByUserIdAndBeneficiaryVpa(request.getUserId(), request.getBeneficiaryVpa())
                .map(existing -> ResponseEntity.ok(existing))
                .orElseGet(() -> {
                    Beneficiary saved = beneficiaryRepository.save(request);
                    return ResponseEntity.status(201).body(saved);
                });
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List beneficiaries", description = "Get all saved beneficiaries for a user")
    public ResponseEntity<List<Beneficiary>> list(@PathVariable UUID userId) {
        return ResponseEntity.ok(beneficiaryRepository.findByUserIdOrderByLastPaidAtDesc(userId));
    }

    @GetMapping("/user/{userId}/frequent")
    @Operation(summary = "Frequent beneficiaries", description = "Get top 10 most frequently paid beneficiaries")
    public ResponseEntity<List<Beneficiary>> frequent(@PathVariable UUID userId) {
        return ResponseEntity.ok(beneficiaryRepository.findTop10ByUserIdOrderByTransactionCountDesc(userId));
    }

    @GetMapping("/user/{userId}/favorites")
    @Operation(summary = "Favorite beneficiaries", description = "Get favorite beneficiaries")
    public ResponseEntity<List<Beneficiary>> favorites(@PathVariable UUID userId) {
        return ResponseEntity.ok(beneficiaryRepository.findByUserIdAndIsFavoriteTrueOrderByBeneficiaryName(userId));
    }

    @PutMapping("/{id}/favorite")
    @Operation(summary = "Toggle favorite", description = "Mark or unmark a beneficiary as favorite")
    public ResponseEntity<Beneficiary> toggleFavorite(@PathVariable UUID id) {
        Beneficiary b = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
        b.setIsFavorite(!b.getIsFavorite());
        return ResponseEntity.ok(beneficiaryRepository.save(b));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove beneficiary", description = "Delete a saved beneficiary")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        beneficiaryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Beneficiary removed"));
    }

    /**
     * Record a payment to a beneficiary (called after successful transaction).
     */
    public void recordPayment(UUID userId, String vpa) {
        beneficiaryRepository.findByUserIdAndBeneficiaryVpa(userId, vpa).ifPresent(b -> {
            b.setLastPaidAt(Instant.now());
            b.setTransactionCount(b.getTransactionCount() + 1);
            beneficiaryRepository.save(b);
        });
    }
}
