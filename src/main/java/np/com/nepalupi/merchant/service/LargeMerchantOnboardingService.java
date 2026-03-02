package np.com.nepalupi.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.merchant.dto.LargeMerchantOnboardRequest;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.enums.MerchantCategory;
import np.com.nepalupi.merchant.enums.MerchantStatus;
import np.com.nepalupi.merchant.enums.MerchantType;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import np.com.nepalupi.repository.VpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Large Merchant Onboarding — Indian UPI model.
 * <p>
 * Large merchants (e-commerce, utility companies, large retail chains)
 * require PAN/registration docs, API key provisioning, webhook setup,
 * and go through a review process before activation.
 * MDR may apply based on NRB/NPCI guidelines.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LargeMerchantOnboardingService {

    private final MerchantRepository merchantRepository;
    private final QrCodeService qrCodeService;
    private final VpaRepository vpaRepository;

    @Transactional
    public Merchant onboard(LargeMerchantOnboardRequest request) {
        log.info("Onboarding large merchant: {}", request.getBusinessName());

        if (merchantRepository.existsByMerchantVpa(request.getDesiredVpa())) {
            throw new IllegalStateException("Merchant VPA already taken: " + request.getDesiredVpa());
        }

        MerchantCategory category = MerchantCategory.valueOf(request.getBusinessCategory());
        String merchantId = "MER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Generate API credentials
        String apiKey = generateApiKey();
        String apiSecret = generateApiSecret();

        // Hash document
        String docHash = request.getRegistrationDocBase64() != null
                ? hashDocument(request.getRegistrationDocBase64()) : null;

        Merchant merchant = Merchant.builder()
                .merchantId(merchantId)
                .userId(request.getUserId())
                .businessName(request.getBusinessName())
                .businessCategory(category)
                .mccCode(category.getMccCode())
                .merchantType(MerchantType.LARGE)
                .merchantVpa(request.getDesiredVpa())
                .panNumber(request.getPanNumber())
                .registrationDocHash(docHash)
                .apiKeyHash(hashString(apiKey))
                .apiSecretHash(hashString(apiSecret))
                .webhookUrl(request.getWebhookUrl())
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .district(request.getDistrict())
                .settlementAccountId(request.getBankAccountId())
                .settlementCycle(request.getSettlementCycle() != null ? request.getSettlementCycle() : "T1")
                .mdrPercent(new BigDecimal("0.0030"))  // 0.30% default MDR for large merchants
                .pushEnabled(true)
                .audioNotification(false)
                .status(MerchantStatus.PENDING) // Large merchants need review
                .build();

        merchant = merchantRepository.save(merchant);

        // Generate static QR (will be active after approval)
        String staticQr = qrCodeService.generateStaticQr(merchant);
        merchant.setStaticQrData(staticQr);
        merchant = merchantRepository.save(merchant);

        log.info("Large merchant submitted for review: id={} vpa={}", merchantId, request.getDesiredVpa());
        // In production, return API key + secret to merchant (one-time only)
        return merchant;
    }

    /**
     * Approve a pending large merchant.
     */
    @Transactional
    public Merchant approve(UUID merchantDbId) {
        Merchant merchant = merchantRepository.findById(merchantDbId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        if (merchant.getStatus() != MerchantStatus.PENDING) {
            throw new IllegalStateException("Merchant not in PENDING state");
        }

        merchant.setStatus(MerchantStatus.ACTIVE);
        merchant = merchantRepository.save(merchant);

        // Create VPA record so transaction engine can resolve it
        if (!vpaRepository.existsByVpaAddress(merchant.getMerchantVpa())) {
            Vpa vpa = Vpa.builder()
                    .vpaAddress(merchant.getMerchantVpa())
                    .userId(merchant.getUserId())
                    .bankAccountId(merchant.getSettlementAccountId() != null
                            ? merchant.getSettlementAccountId()
                            : merchant.getId())
                    .bankCode(merchant.getMerchantVpa().split("@")[1].toUpperCase())
                    .isPrimary(true)
                    .isActive(true)
                    .build();
            vpaRepository.save(vpa);
            log.info("Merchant VPA entry created: {}", merchant.getMerchantVpa());
        }

        log.info("Large merchant approved: id={}", merchant.getMerchantId());
        return merchant;
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "npupi_" + HexFormat.of().formatHex(bytes);
    }

    private String generateApiSecret() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashString(String input) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String hashDocument(String base64Doc) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Doc);
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(decoded));
        } catch (Exception e) {
            log.warn("Failed to hash document: {}", e.getMessage());
            return "HASH_FAILED_" + System.currentTimeMillis();
        }
    }
}
