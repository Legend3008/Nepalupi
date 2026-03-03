package np.com.nepalupi.service.vpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.repository.UserRepository;
import np.com.nepalupi.repository.VpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves a mobile number to the user's primary VPA address.
 * Enables send-via-mobile functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MobileVpaLookupService {

    private final UserRepository userRepository;
    private final VpaRepository vpaRepository;

    /**
     * Resolve mobile number → primary VPA address.
     *
     * @param mobileNumber Nepal mobile number (e.g., "9841234567")
     * @return Map containing resolved VPA and user details
     */
    public Map<String, Object> resolveByMobile(String mobileNumber) {
        // Normalize: strip +977 prefix if present
        String normalized = mobileNumber.replaceAll("^\\+?977", "").trim();

        if (!normalized.matches("^(98|97|96)\\d{8}$")) {
            throw new IllegalArgumentException("Invalid Nepal mobile number: " + mobileNumber);
        }

        User user = userRepository.findByMobileNumber(normalized)
                .orElseThrow(() -> new IllegalArgumentException("No NUPI account linked to mobile: " + normalized));

        // Get primary VPA
        List<Vpa> vpas = vpaRepository.findByUserIdAndIsActiveTrue(user.getId());
        Optional<Vpa> primaryVpa = vpas.stream()
                .filter(Vpa::getIsPrimary)
                .findFirst();

        Vpa resolvedVpa = primaryVpa.orElse(vpas.isEmpty() ? null : vpas.get(0));

        if (resolvedVpa == null) {
            throw new IllegalStateException("No active VPA found for mobile: " + normalized);
        }

        log.info("Resolved mobile {} → VPA {}", normalized, resolvedVpa.getVpaAddress());

        return Map.of(
                "mobile", normalized,
                "vpa", resolvedVpa.getVpaAddress(),
                "name", user.getFullName() != null ? user.getFullName() : "",
                "bankCode", resolvedVpa.getBankCode(),
                "verified", true
        );
    }
}
