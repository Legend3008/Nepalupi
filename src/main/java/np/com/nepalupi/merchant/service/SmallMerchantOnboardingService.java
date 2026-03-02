package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.merchant.dto.SmallMerchantOnboardRequest;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.enums.MerchantCategory;
import np.com.nepalupi.merchant.enums.MerchantStatus;
import np.com.nepalupi.merchant.enums.MerchantType;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import np.com.nepalupi.repository.VpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Small Merchant Onboarding — Indian UPI model.
 * <p>
 * In India, small merchants (kirana stores, tea stalls) are onboarded
 * with minimal paperwork — just KYC'd mobile number + business name.
 * They get a static QR printed from a sticker, zero MDR, T+1 settlement.
 * <p>
 * Nepal adaptation: Same model for Nepal's small retail ecosystem.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmallMerchantOnboardingService {

    private final MerchantRepository merchantRepository;
    private final QrCodeService qrCodeService;
    private final VpaRepository vpaRepository;

    @Transactional
    public Merchant onboard(SmallMerchantOnboardRequest request) {
        log.info("Onboarding small merchant: {}", request.getBusinessName());

        if (merchantRepository.existsByMerchantVpa(request.getDesiredVpa())) {
            throw new IllegalStateException("Merchant VPA already taken: " + request.getDesiredVpa());
        }

        MerchantCategory category = MerchantCategory.valueOf(request.getBusinessCategory());
        String merchantId = "MER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Merchant merchant = Merchant.builder()
                .merchantId(merchantId)
                .userId(request.getUserId())
                .businessName(request.getBusinessName())
                .businessCategory(category)
                .mccCode(category.getMccCode())
                .merchantType(MerchantType.SMALL)
                .merchantVpa(request.getDesiredVpa())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .district(request.getDistrict())
                .settlementAccountId(request.getBankAccountId())
                .settlementCycle("T1")
                .mdrPercent(BigDecimal.ZERO)  // Zero MDR for small merchants
                .pushEnabled(true)
                .audioNotification(true)      // "Paisa aayo!" audio for small merchants
                .status(MerchantStatus.ACTIVE) // Small merchants auto-activated
                .build();

        merchant = merchantRepository.save(merchant);

        // Auto-generate static QR
        String staticQr = qrCodeService.generateStaticQr(merchant);
        merchant.setStaticQrData(staticQr);
        merchant = merchantRepository.save(merchant);

        // Create VPA record so transaction engine can resolve it
        if (!vpaRepository.existsByVpaAddress(request.getDesiredVpa())) {
            Vpa vpa = Vpa.builder()
                    .vpaAddress(request.getDesiredVpa())
                    .userId(request.getUserId())
                    .bankAccountId(request.getBankAccountId() != null
                            ? request.getBankAccountId()
                            : merchant.getId()) // use merchant ID as placeholder
                    .bankCode(request.getDesiredVpa().split("@")[1].toUpperCase())
                    .isPrimary(true)
                    .isActive(true)
                    .build();
            vpaRepository.save(vpa);
            log.info("Merchant VPA entry created: {}", request.getDesiredVpa());
        }

        log.info("Small merchant onboarded: id={} vpa={}", merchantId, request.getDesiredVpa());
        return merchant;
    }
}
