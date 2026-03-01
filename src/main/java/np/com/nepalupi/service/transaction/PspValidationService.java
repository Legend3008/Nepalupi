package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.exception.PspValidationException;
import np.com.nepalupi.repository.PspRepository;
import org.springframework.stereotype.Service;

/**
 * Validates that a PSP (Payment Service Provider) is registered and active.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspValidationService {

    private final PspRepository pspRepository;

    public void validate(String pspId) {
        if (pspId == null || pspId.isBlank()) {
            throw new PspValidationException("PSP ID is required");
        }

        pspRepository.findByPspIdAndIsActiveTrue(pspId)
                .orElseThrow(() -> new PspValidationException(
                        "PSP not registered or inactive: " + pspId));

        log.debug("PSP validated: {}", pspId);
    }
}
