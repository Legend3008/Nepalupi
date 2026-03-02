package np.com.nepalupi.merchant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.merchant.dto.DynamicQrRequest;
import np.com.nepalupi.merchant.dto.LargeMerchantOnboardRequest;
import np.com.nepalupi.merchant.dto.SmallMerchantOnboardRequest;
import np.com.nepalupi.merchant.entity.Merchant;
import np.com.nepalupi.merchant.entity.MerchantQrCode;
import np.com.nepalupi.merchant.entity.MerchantSettlement;
import np.com.nepalupi.merchant.repository.MerchantRepository;
import np.com.nepalupi.merchant.service.LargeMerchantOnboardingService;
import np.com.nepalupi.merchant.service.MerchantSettlementService;
import np.com.nepalupi.merchant.service.QrCodeService;
import np.com.nepalupi.merchant.service.SmallMerchantOnboardingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Merchant Controller — Module 10.
 * <p>
 * POST /api/v1/merchants/small/onboard        → Small merchant self-onboarding
 * POST /api/v1/merchants/large/onboard         → Large merchant onboarding
 * POST /api/v1/merchants/large/{id}/approve     → Approve large merchant
 * POST /api/v1/merchants/{id}/suspend           → Suspend merchant
 * GET  /api/v1/merchants/{id}                   → Get merchant details
 * GET  /api/v1/merchants/vpa/{vpa}              → Lookup by VPA
 * POST /api/v1/merchants/qr/dynamic             → Generate dynamic QR
 * GET  /api/v1/merchants/qr/resolve/{txnRef}    → Resolve QR by txn ref
 * GET  /api/v1/merchants/{id}/settlements       → Settlement history
 */
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
@Tag(name = "Merchants", description = "Merchant onboarding, QR codes & settlement")
public class MerchantController {

    private final SmallMerchantOnboardingService smallOnboardingService;
    private final LargeMerchantOnboardingService largeOnboardingService;
    private final QrCodeService qrCodeService;
    private final MerchantSettlementService settlementService;
    private final MerchantRepository merchantRepository;

    // ── Onboarding ──

    @PostMapping("/small/onboard")
    public ResponseEntity<Merchant> onboardSmallMerchant(
            @Valid @RequestBody SmallMerchantOnboardRequest request) {
        return ResponseEntity.ok(smallOnboardingService.onboard(request));
    }

    @PostMapping("/large/onboard")
    public ResponseEntity<Merchant> onboardLargeMerchant(
            @Valid @RequestBody LargeMerchantOnboardRequest request) {
        return ResponseEntity.ok(largeOnboardingService.onboard(request));
    }

    @PostMapping("/large/{id}/approve")
    public ResponseEntity<Merchant> approveLargeMerchant(@PathVariable UUID id) {
        return ResponseEntity.ok(largeOnboardingService.approve(id));
    }

    // ── Merchant Management ──

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> getMerchant(@PathVariable UUID id) {
        return merchantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vpa/{vpa}")
    public ResponseEntity<Merchant> getMerchantByVpa(@PathVariable String vpa) {
        return merchantRepository.findByMerchantVpa(vpa)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<Merchant> suspendMerchant(
            @PathVariable UUID id,
            @RequestParam String reason) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        merchant.setStatus(np.com.nepalupi.merchant.enums.MerchantStatus.SUSPENDED);
        merchant.setSuspendedReason(reason);
        return ResponseEntity.ok(merchantRepository.save(merchant));
    }

    // ── QR Codes ──

    @PostMapping("/qr/dynamic")
    public ResponseEntity<MerchantQrCode> generateDynamicQr(
            @Valid @RequestBody DynamicQrRequest request) {
        return ResponseEntity.ok(qrCodeService.generateDynamicQr(request));
    }

    @GetMapping("/qr/resolve/{txnRef}")
    public ResponseEntity<MerchantQrCode> resolveQr(@PathVariable String txnRef) {
        return ResponseEntity.ok(qrCodeService.resolveByTxnRef(txnRef));
    }

    // ── Settlements ──

    @GetMapping("/{id}/settlements")
    public ResponseEntity<List<MerchantSettlement>> getSettlements(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(settlementService.getSettlementHistory(id, from, to));
    }
}
