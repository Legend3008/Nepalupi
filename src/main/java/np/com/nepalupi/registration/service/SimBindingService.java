package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.registration.entity.DeviceBinding;
import np.com.nepalupi.registration.enums.DeviceBindingStatus;
import np.com.nepalupi.registration.repository.DeviceBindingRepository;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * SIM Binding Service — Step 1 of UPI registration.
 * <p>
 * Indian UPI model: PSP app collects mobile number, sends a silent binding SMS
 * to the UPI switch short-code. The SMS carries a unique token; when the switch
 * receives it, the phone number is verified and the device is bound.
 * <p>
 * Nepal adaptation: Uses Nepal Telecom / Ncell SMS gateway.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimBindingService {

    private final DeviceBindingRepository deviceBindingRepository;
    private final UserRepository userRepository;

    /**
     * Initiate SIM binding — generate a unique SMS token, persist a PENDING
     * binding record, and instruct the PSP app to fire the silent SMS.
     */
    @Transactional
    public DeviceBinding initiateSmsBinding(String mobileNumber, String deviceId,
                                            String imei, String simSerial) {
        log.info("Initiating SIM binding for mobile={}", mobileNumber);

        // Check for existing verified binding
        if (deviceBindingRepository.existsByMobileNumberAndStatus(
                mobileNumber, DeviceBindingStatus.VERIFIED)) {
            throw new IllegalStateException("Mobile number already bound. Use device-change flow.");
        }

        String bindingSmsId = "SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        DeviceBinding binding = DeviceBinding.builder()
                .mobileNumber(mobileNumber)
                .deviceId(deviceId)
                .imei(imei)
                .simSerial(simSerial)
                .bindingSmsId(bindingSmsId)
                .status(DeviceBindingStatus.SMS_SENT)
                .expiresAt(Instant.now().plusSeconds(300)) // 5-minute expiry
                .build();

        binding = deviceBindingRepository.save(binding);
        log.info("Binding SMS token={} generated for mobile={}", bindingSmsId, mobileNumber);

        // In production: trigger silent SMS via Nepal Telecom / Ncell gateway here
        return binding;
    }

    /**
     * Verify binding when the switch receives the silent SMS with the token.
     */
    @Transactional
    public DeviceBinding verifyBinding(String bindingSmsId) {
        DeviceBinding binding = deviceBindingRepository.findByBindingSmsId(bindingSmsId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown SMS binding token"));

        if (binding.getStatus() == DeviceBindingStatus.VERIFIED) {
            return binding; // idempotent
        }
        if (binding.getStatus() != DeviceBindingStatus.SMS_SENT) {
            throw new IllegalStateException("Binding in invalid state: " + binding.getStatus());
        }
        if (Instant.now().isAfter(binding.getExpiresAt())) {
            binding.setStatus(DeviceBindingStatus.EXPIRED);
            deviceBindingRepository.save(binding);
            throw new IllegalStateException("Binding SMS token expired");
        }

        binding.setStatus(DeviceBindingStatus.VERIFIED);
        binding.setBoundAt(Instant.now());

        // Link to user or create user record
        User user = userRepository.findByMobileNumber(binding.getMobileNumber())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .mobileNumber(binding.getMobileNumber())
                            .fullName("") // to be updated during KYC
                            .deviceId(binding.getDeviceId())
                            .phoneNumberVerified(true)
                            .deviceBound(true)
                            .build();
                    return userRepository.save(newUser);
                });

        user.setPhoneNumberVerified(true);
        user.setDeviceBound(true);
        user.setDeviceId(binding.getDeviceId());
        userRepository.save(user);

        binding.setUserId(user.getId());
        return deviceBindingRepository.save(binding);
    }
}
