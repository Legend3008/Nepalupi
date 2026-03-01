package np.com.nepalupi.launch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.launch.entity.MerchantAcquisition;
import np.com.nepalupi.launch.enums.AcquisitionChannel;
import np.com.nepalupi.launch.enums.FootfallCategory;
import np.com.nepalupi.launch.repository.MerchantAcquisitionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantAcquisitionService {

    private final MerchantAcquisitionRepository merchantAcquisitionRepository;

    @Transactional
    public MerchantAcquisition onboardMerchant(String merchantName, String city, String category,
                                                FootfallCategory footfallCategory,
                                                AcquisitionChannel channel, String acquiredBy) {
        MerchantAcquisition merchant = MerchantAcquisition.builder()
                .merchantName(merchantName)
                .city(city)
                .category(category)
                .footfallCategory(footfallCategory)
                .acquisitionChannel(channel)
                .acquiredBy(acquiredBy)
                .onboardedAt(Instant.now())
                .build();

        log.info("Onboarding merchant {} in {} via {}", merchantName, city, channel);
        return merchantAcquisitionRepository.save(merchant);
    }

    @Transactional
    public MerchantAcquisition deployQr(UUID merchantId) {
        MerchantAcquisition merchant = merchantAcquisitionRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        merchant.setQrDeployed(true);
        merchant.setQrDeployedAt(Instant.now());
        log.info("QR deployed for merchant {}", merchant.getMerchantName());
        return merchantAcquisitionRepository.save(merchant);
    }

    @Transactional
    public MerchantAcquisition recordFirstTransaction(UUID merchantId) {
        MerchantAcquisition merchant = merchantAcquisitionRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        merchant.setFirstTransactionAt(Instant.now());
        merchant.setIsActive(true);
        log.info("First transaction recorded for merchant {}", merchant.getMerchantName());
        return merchantAcquisitionRepository.save(merchant);
    }

    @Transactional
    public MerchantAcquisition recordChurn(UUID merchantId, String reason) {
        MerchantAcquisition merchant = merchantAcquisitionRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        merchant.setIsActive(false);
        merchant.setChurnedAt(Instant.now());
        merchant.setChurnReason(reason);
        log.warn("Merchant {} churned: {}", merchant.getMerchantName(), reason);
        return merchantAcquisitionRepository.save(merchant);
    }

    public List<MerchantAcquisition> getActiveMerchants() {
        return merchantAcquisitionRepository.findByIsActiveTrue();
    }

    public List<MerchantAcquisition> getMerchantsByCity(String city) {
        return merchantAcquisitionRepository.findByCity(city);
    }

    public List<MerchantAcquisition> getQrDeployedNoTransaction() {
        return merchantAcquisitionRepository.findQrDeployedButNoTransaction();
    }

    public List<MerchantAcquisition> getChurnedMerchants() {
        return merchantAcquisitionRepository.findChurnedMerchants();
    }

    public List<Object[]> getMerchantCountByCity() {
        return merchantAcquisitionRepository.countActiveMerchantsByCity();
    }

    // ─── Scheduled: Identify inactive merchants at risk of churn ────

    @Scheduled(cron = "0 0 6 * * MON") // Every Monday at 6 AM
    public void weeklyChurnRiskCheck() {
        log.info("Running weekly merchant churn risk check");
        List<MerchantAcquisition> qrNoTxn = merchantAcquisitionRepository.findQrDeployedButNoTransaction();
        if (!qrNoTxn.isEmpty()) {
            log.warn("{} merchants have QR deployed but no transactions — churn risk!", qrNoTxn.size());
        }
    }
}
