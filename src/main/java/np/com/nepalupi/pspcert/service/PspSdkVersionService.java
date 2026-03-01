package np.com.nepalupi.pspcert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.pspcert.entity.PspSdkVersion;
import np.com.nepalupi.pspcert.repository.PspSdkVersionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Tracks SDK versions across all PSP apps. Sends upgrade notices when
 * new SDK versions are released. Restricts transactions for PSPs that
 * fail to adopt critical security updates past their deadline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PspSdkVersionService {

    private final PspSdkVersionRepository sdkRepo;

    @Transactional
    public PspSdkVersion registerSdkVersion(String pspId, String platform,
                                              String currentVersion, String latestVersion) {
        PspSdkVersion sdk = PspSdkVersion.builder()
                .pspId(pspId)
                .appPlatform(np.com.nepalupi.pspcert.enums.AppPlatform.valueOf(platform))
                .currentSdkVersion(currentVersion)
                .latestAvailableVersion(latestVersion)
                .isCurrent(currentVersion.equals(latestVersion))
                .upgradeRequired(!currentVersion.equals(latestVersion))
                .lastCheckedAt(Instant.now())
                .build();

        if (sdk.getUpgradeRequired()) {
            sdk.setUpgradeDeadline(Instant.now().plus(30, ChronoUnit.DAYS));
        }

        return sdkRepo.save(sdk);
    }

    @Transactional
    public PspSdkVersion notifyUpgrade(UUID sdkId) {
        PspSdkVersion sdk = sdkRepo.findById(sdkId)
                .orElseThrow(() -> new IllegalArgumentException("SDK version record not found"));
        sdk.setUpgradeNoticeSent(true);
        sdk.setUpgradeNoticeSentAt(Instant.now());
        log.info("SDK upgrade notice sent: psp={}, current={}, latest={}",
                sdk.getPspId(), sdk.getCurrentSdkVersion(), sdk.getLatestAvailableVersion());
        return sdkRepo.save(sdk);
    }

    @Transactional
    public PspSdkVersion confirmUpgrade(UUID sdkId, String newVersion) {
        PspSdkVersion sdk = sdkRepo.findById(sdkId)
                .orElseThrow(() -> new IllegalArgumentException("SDK version record not found"));
        sdk.setCurrentSdkVersion(newVersion);
        sdk.setIsCurrent(newVersion.equals(sdk.getLatestAvailableVersion()));
        sdk.setUpgradeRequired(!newVersion.equals(sdk.getLatestAvailableVersion()));
        sdk.setTransactionsRestricted(false);
        sdk.setLastCheckedAt(Instant.now());
        log.info("SDK upgraded: psp={}, version={}", sdk.getPspId(), newVersion);
        return sdkRepo.save(sdk);
    }

    /**
     * Daily: check for PSPs past their SDK upgrade deadline. Restrict transactions
     * for those that haven't upgraded.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void enforceUpgradeDeadlines() {
        List<PspSdkVersion> pastDeadline = sdkRepo.findPastUpgradeDeadline();
        for (PspSdkVersion sdk : pastDeadline) {
            sdk.setTransactionsRestricted(true);
            sdk.setRestrictedAt(Instant.now());
            sdkRepo.save(sdk);
            log.warn("TRANSACTIONS RESTRICTED: psp={} failed to upgrade SDK by deadline. current={}, required={}",
                    sdk.getPspId(), sdk.getCurrentSdkVersion(), sdk.getLatestAvailableVersion());
        }
    }

    public List<PspSdkVersion> getOutdatedSdks() {
        return sdkRepo.findByIsCurrentFalseOrderByPspId();
    }

    public List<PspSdkVersion> getRequiringUpgrade() {
        return sdkRepo.findRequiringUpgrade();
    }

    public List<PspSdkVersion> getSdkVersionsForPsp(String pspId) {
        return sdkRepo.findByPspIdOrderByCreatedAtDesc(pspId);
    }
}
