package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.registration.entity.DeviceBinding;
import np.com.nepalupi.registration.entity.DeviceChangeRequest;
import np.com.nepalupi.registration.enums.DeviceBindingStatus;
import np.com.nepalupi.registration.enums.DeviceChangeStatus;
import np.com.nepalupi.registration.repository.DeviceBindingRepository;
import np.com.nepalupi.registration.repository.DeviceChangeRequestRepository;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Device Change / Account Recovery Service.
 * <p>
 * Indian UPI model:
 * - User gets a new phone → needs to re-bind device
 * - Requires MPIN verification on old device (if possible) or OTP fallback
 * - 24-hour cooling period before new device becomes active
 * - Prevents account takeover attacks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceChangeService {

    private static final int COOLING_PERIOD_HOURS = 24;

    private final DeviceChangeRequestRepository changeRequestRepository;
    private final DeviceBindingRepository deviceBindingRepository;
    private final UserRepository userRepository;

    /**
     * Initiate device change request.
     */
    @Transactional
    public DeviceChangeRequest initiateDeviceChange(UUID userId, String newDeviceId,
                                                     String newSimSerial, String encryptedMpin) {
        log.info("Initiating device change for user={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check no pending change request
        boolean hasPending = changeRequestRepository.existsByUserIdAndStatusIn(
                userId, List.of(DeviceChangeStatus.PENDING_VERIFICATION, DeviceChangeStatus.COOLING_PERIOD));
        if (hasPending) {
            throw new IllegalStateException("A device change request is already in progress");
        }

        // In production: verify MPIN with issuer bank
        // For simulation, assume MPIN is valid
        boolean mpinValid = verifyMpinWithBank(userId, encryptedMpin);

        DeviceChangeRequest request = DeviceChangeRequest.builder()
                .userId(userId)
                .oldDeviceId(user.getDeviceId())
                .newDeviceId(newDeviceId)
                .newSimSerial(newSimSerial)
                .mpinVerified(mpinValid)
                .status(mpinValid ? DeviceChangeStatus.COOLING_PERIOD : DeviceChangeStatus.PENDING_VERIFICATION)
                .coolingEndsAt(mpinValid ? Instant.now().plusSeconds(COOLING_PERIOD_HOURS * 3600L) : null)
                .build();

        request = changeRequestRepository.save(request);

        if (mpinValid) {
            log.info("Device change MPIN verified for user={}. Cooling period until {}",
                    userId, request.getCoolingEndsAt());
        } else {
            log.warn("Device change MPIN verification failed for user={}", userId);
        }

        return request;
    }

    /**
     * Complete device change after cooling period expires.
     */
    @Transactional
    public DeviceChangeRequest completeDeviceChange(UUID requestId) {
        DeviceChangeRequest request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Change request not found"));

        if (request.getStatus() != DeviceChangeStatus.COOLING_PERIOD) {
            throw new IllegalStateException("Request not in COOLING_PERIOD state");
        }
        if (Instant.now().isBefore(request.getCoolingEndsAt())) {
            throw new IllegalStateException("Cooling period has not ended. Ends at: " + request.getCoolingEndsAt());
        }

        // Update user's device
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setDeviceId(request.getNewDeviceId());
        user.setLastDeviceChangeAt(Instant.now());
        userRepository.save(user);

        // Create new device binding
        DeviceBinding newBinding = DeviceBinding.builder()
                .userId(user.getId())
                .mobileNumber(user.getMobileNumber())
                .deviceId(request.getNewDeviceId())
                .simSerial(request.getNewSimSerial())
                .status(DeviceBindingStatus.VERIFIED)
                .boundAt(Instant.now())
                .build();
        deviceBindingRepository.save(newBinding);

        // Mark old bindings as expired
        String newDeviceId = request.getNewDeviceId();
        deviceBindingRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(b -> !b.getDeviceId().equals(newDeviceId))
                .filter(b -> b.getStatus() == DeviceBindingStatus.VERIFIED)
                .forEach(b -> {
                    b.setStatus(DeviceBindingStatus.EXPIRED);
                    deviceBindingRepository.save(b);
                });

        request.setStatus(DeviceChangeStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        DeviceChangeRequest savedRequest = changeRequestRepository.save(request);

        log.info("Device change completed for user={}. New device={}", user.getId(), savedRequest.getNewDeviceId());
        return savedRequest;
    }

    private boolean verifyMpinWithBank(UUID userId, String encryptedMpin) {
        // In production: forward to issuer bank via secure channel
        // Simulated: always true
        return true;
    }
}
