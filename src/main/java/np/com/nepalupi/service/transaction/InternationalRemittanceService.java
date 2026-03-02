package np.com.nepalupi.service.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.config.FeatureFlagService;
import np.com.nepalupi.domain.entity.InternationalRemittance;
import np.com.nepalupi.repository.InternationalRemittanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Section 19.2: UPI International — cross-border remittances.
 * <p>
 * Nepal-specific corridors:
 * - Nepal ↔ India (NPR-INR via ConnectIPS/NCHL channel)
 * - Nepal ↔ Singapore (NPR-SGD via PayNow partnership)
 * - Nepal ↔ Gulf states (NPR-AED, NPR-SAR for remittance)
 * <p>
 * Currency conversion at NCHL/NRB level.
 * Compliance: NRB foreign exchange regulations apply.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InternationalRemittanceService {

    private final InternationalRemittanceRepository remittanceRepository;
    private final FeatureFlagService featureFlagService;

    // Exchange rates (would come from NRB daily fixing in production)
    private static final Map<String, BigDecimal> EXCHANGE_RATES = Map.of(
            "NPR_INR", new BigDecimal("0.625"),      // 1 NPR = 0.625 INR (approx)
            "NPR_USD", new BigDecimal("0.0075"),      // 1 NPR = 0.0075 USD
            "NPR_SGD", new BigDecimal("0.0101"),      // 1 NPR = 0.0101 SGD
            "NPR_AED", new BigDecimal("0.0276"),      // 1 NPR = 0.0276 AED
            "NPR_SAR", new BigDecimal("0.0282"),      // 1 NPR = 0.0282 SAR
            "NPR_GBP", new BigDecimal("0.0059"),      // 1 NPR = 0.0059 GBP
            "NPR_MYR", new BigDecimal("0.0334")       // 1 NPR = 0.0334 MYR
    );

    // Partner systems per corridor
    private static final Map<String, String> CORRIDOR_PARTNERS = Map.of(
            "IND", "CONNECTIPS",
            "SGP", "PAYNOW",
            "ARE", "NCHL_REMIT",
            "SAU", "NCHL_REMIT",
            "USA", "NCHL_REMIT",
            "GBR", "NCHL_REMIT",
            "MYS", "NCHL_REMIT"
    );

    /**
     * Initiate international remittance.
     */
    @Transactional
    public Map<String, Object> initiateRemittance(String payerVpa, String payeeIdentifier,
                                                    String destCountry, long sourceAmountPaisa) {
        if (!featureFlagService.isInternationalEnabled()) {
            throw new IllegalStateException("International UPI is not yet available in Nepal");
        }

        String destCurrency = getCountryCurrency(destCountry);
        String rateKey = "NPR_" + destCurrency;
        BigDecimal rate = EXCHANGE_RATES.get(rateKey);

        if (rate == null) {
            throw new IllegalArgumentException("Unsupported corridor: Nepal → " + destCountry);
        }

        // Convert amount
        BigDecimal sourceAmount = BigDecimal.valueOf(sourceAmountPaisa).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal destAmount = sourceAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        long destAmountMinor = destAmount.movePointRight(2).longValue();

        // NRB compliance check (max NPR 100,000 per txn for remittance)
        if (sourceAmountPaisa > 10_000_000L) {
            throw new IllegalStateException("International remittance exceeds NRB per-transaction limit of NPR 100,000");
        }

        String partnerSystem = CORRIDOR_PARTNERS.getOrDefault(destCountry, "NCHL_REMIT");

        InternationalRemittance remittance = InternationalRemittance.builder()
                .sourceCountry("NPL")
                .destCountry(destCountry)
                .sourceCurrency("NPR")
                .destCurrency(destCurrency)
                .sourceAmountMinor(sourceAmountPaisa)
                .destAmountMinor(destAmountMinor)
                .exchangeRate(rate)
                .payerVpa(payerVpa)
                .payeeIdentifier(payeeIdentifier)
                .partnerSystem(partnerSystem)
                .status("INITIATED")
                .complianceCheckStatus("PENDING")
                .build();

        remittance = remittanceRepository.save(remittance);

        log.info("International remittance initiated: id={}, {}->{}, NPR {}→{} {}, partner={}",
                remittance.getId(), "NPL", destCountry, sourceAmount, destAmount, destCurrency, partnerSystem);

        return Map.of(
                "remittanceId", remittance.getId().toString(),
                "status", "INITIATED",
                "sourceAmount", sourceAmountPaisa,
                "sourceCurrency", "NPR",
                "destAmount", destAmountMinor,
                "destCurrency", destCurrency,
                "exchangeRate", rate.toString(),
                "partnerSystem", partnerSystem,
                "complianceCheck", "PENDING"
        );
    }

    /**
     * Get remittance status.
     */
    public InternationalRemittance getRemittanceStatus(UUID remittanceId) {
        return remittanceRepository.findById(remittanceId)
                .orElseThrow(() -> new IllegalArgumentException("Remittance not found: " + remittanceId));
    }

    /**
     * List remittances for a payer.
     */
    public List<InternationalRemittance> getPayerRemittances(String payerVpa) {
        return remittanceRepository.findByPayerVpaOrderByCreatedAtDesc(payerVpa);
    }

    /**
     * Get available corridors and exchange rates.
     */
    public Map<String, Object> getAvailableCorridors() {
        return Map.of(
                "corridors", CORRIDOR_PARTNERS,
                "exchangeRates", EXCHANGE_RATES,
                "baseCurrency", "NPR",
                "nrbPerTxnLimit", 10_000_000L,
                "note", "Exchange rates are indicative. Final rate applied at time of settlement."
        );
    }

    /**
     * Complete remittance after compliance check.
     */
    @Transactional
    public void completeRemittance(UUID remittanceId) {
        InternationalRemittance remittance = remittanceRepository.findById(remittanceId)
                .orElseThrow(() -> new IllegalArgumentException("Remittance not found"));

        remittance.setStatus("COMPLETED");
        remittance.setComplianceCheckStatus("CLEARED");
        remittance.setCompletedAt(Instant.now());
        remittanceRepository.save(remittance);

        log.info("International remittance completed: id={}", remittanceId);
    }

    private String getCountryCurrency(String countryCode) {
        return switch (countryCode) {
            case "IND" -> "INR";
            case "USA" -> "USD";
            case "SGP" -> "SGD";
            case "ARE" -> "AED";
            case "SAU" -> "SAR";
            case "GBR" -> "GBP";
            case "MYS" -> "MYR";
            default -> throw new IllegalArgumentException("Unsupported country: " + countryCode);
        };
    }
}
