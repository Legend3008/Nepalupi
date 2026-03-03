package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.SoundboxDevice;
import np.com.nepalupi.repository.SoundboxDeviceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Soundbox device management and payment notification service.
 * Hardware devices at merchant locations that announce payment confirmations via audio.
 * Supports multiple languages (Nepali, English, Hindi).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SoundboxService {

    private final SoundboxDeviceRepository soundboxRepository;

    /**
     * Register a new soundbox device for a merchant.
     */
    public SoundboxDevice registerDevice(UUID merchantId, String deviceSerial, String deviceModel,
                                          String firmwareVersion, String simNumber) {
        // Check for duplicate serial
        soundboxRepository.findByDeviceSerial(deviceSerial).ifPresent(d -> {
            throw new IllegalStateException("Device already registered: " + deviceSerial);
        });

        SoundboxDevice device = SoundboxDevice.builder()
                .merchantId(merchantId)
                .deviceSerial(deviceSerial)
                .deviceModel(deviceModel != null ? deviceModel : "NUPI-SB100")
                .firmwareVersion(firmwareVersion != null ? firmwareVersion : "1.0.0")
                .simNumber(simNumber)
                .isActive(true)
                .language("ne")
                .volumeLevel(80)
                .lastHeartbeatAt(Instant.now())
                .build();

        soundboxRepository.save(device);
        log.info("Soundbox registered: merchantId={}, serial={}", merchantId, deviceSerial);
        return device;
    }

    /**
     * Send payment notification to all active soundbox devices for a merchant.
     * In production: push via MQTT/WebSocket to the IoT device.
     */
    public void notifyPayment(UUID merchantId, Long amountPaisa, String payerVpa) {
        List<SoundboxDevice> devices = soundboxRepository.findByMerchantIdAndIsActiveTrue(merchantId);

        for (SoundboxDevice device : devices) {
            String announcement = generateAnnouncement(amountPaisa, payerVpa, device.getLanguage());
            // In production: send via MQTT to device
            log.info("Soundbox notification: serial={}, announcement={}", device.getDeviceSerial(), announcement);
        }
    }

    /**
     * Generate the audio announcement text.
     */
    public String generateAnnouncement(Long amountPaisa, String payerVpa, String language) {
        double amountRs = amountPaisa / 100.0;
        return switch (language) {
            case "en" -> String.format("Payment received. Rupees %.2f from %s via NUPI.", amountRs, payerVpa);
            case "hi" -> String.format("भुगतान प्राप्त। रुपये %.2f %s से NUPI के माध्यम से।", amountRs, payerVpa);
            default -> String.format("भुक्तानी प्राप्त। रु %.2f %s बाट NUPI मार्फत।", amountRs, payerVpa);
        };
    }

    /**
     * Update device heartbeat (called periodically by device).
     */
    public SoundboxDevice heartbeat(String deviceSerial) {
        SoundboxDevice device = soundboxRepository.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceSerial));
        device.setLastHeartbeatAt(Instant.now());
        soundboxRepository.save(device);
        return device;
    }

    /**
     * Update device settings.
     */
    public SoundboxDevice updateSettings(UUID deviceId, String language, Integer volumeLevel) {
        SoundboxDevice device = soundboxRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        if (language != null) device.setLanguage(language);
        if (volumeLevel != null) device.setVolumeLevel(Math.min(100, Math.max(0, volumeLevel)));
        soundboxRepository.save(device);
        return device;
    }

    /**
     * Deactivate a device.
     */
    public void deactivateDevice(UUID deviceId) {
        SoundboxDevice device = soundboxRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        device.setIsActive(false);
        soundboxRepository.save(device);
        log.info("Soundbox deactivated: id={}", deviceId);
    }

    /**
     * Get all devices for a merchant.
     */
    public List<SoundboxDevice> getMerchantDevices(UUID merchantId) {
        return soundboxRepository.findByMerchantId(merchantId);
    }

    /**
     * OTA firmware update notification.
     */
    public Map<String, Object> checkFirmwareUpdate(String deviceSerial) {
        SoundboxDevice device = soundboxRepository.findByDeviceSerial(deviceSerial)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceSerial));

        Map<String, Object> update = new HashMap<>();
        update.put("currentVersion", device.getFirmwareVersion());
        update.put("latestVersion", "1.2.0"); // In production: fetch from firmware server
        update.put("updateAvailable", !"1.2.0".equals(device.getFirmwareVersion()));
        update.put("downloadUrl", "https://firmware.nupi.np/soundbox/" + device.getDeviceModel() + "/1.2.0");
        return update;
    }
}
