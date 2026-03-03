package np.com.nepalupi.controller;

import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.*;
import np.com.nepalupi.merchant.service.MerchantRefundService;
import np.com.nepalupi.merchant.service.SoundboxService;
import np.com.nepalupi.service.transaction.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Advanced UPI features controller:
 * - Mobile Recharge
 * - NFC Tap & Pay
 * - IVR / 123Pay
 * - Credit on UPI
 * - Soundbox Management
 * - Merchant Refunds
 */
@RestController
@RequestMapping("/api/v1/advanced")
@RequiredArgsConstructor
public class AdvancedFeaturesController {

    private final MobileRechargeService rechargeService;
    private final NfcPayService nfcPayService;
    private final IvrPayService ivrPayService;
    private final CreditOnUpiService creditOnUpiService;
    private final SoundboxService soundboxService;
    private final MerchantRefundService merchantRefundService;

    // ── Mobile Recharge ──

    @PostMapping("/recharge")
    public ResponseEntity<MobileRecharge> initiateRecharge(@RequestBody Map<String, Object> request) {
        UUID userId = UUID.fromString((String) request.get("userId"));
        String mobile = (String) request.get("mobileNumber");
        Long amount = Long.valueOf(request.get("amountPaisa").toString());
        String type = (String) request.getOrDefault("rechargeType", "PREPAID");
        String planId = (String) request.get("planId");

        return ResponseEntity.ok(rechargeService.initiateRecharge(userId, mobile, amount, type, planId));
    }

    @GetMapping("/recharge/history/{userId}")
    public ResponseEntity<List<MobileRecharge>> rechargeHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(rechargeService.getHistory(userId));
    }

    @GetMapping("/recharge/plans/{operator}")
    public ResponseEntity<List<Map<String, Object>>> rechargePlans(@PathVariable String operator) {
        return ResponseEntity.ok(rechargeService.getPlans(operator));
    }

    @GetMapping("/recharge/detect-operator/{mobile}")
    public ResponseEntity<Map<String, String>> detectOperator(@PathVariable String mobile) {
        String operator = rechargeService.detectOperator(mobile);
        return ResponseEntity.ok(Map.of("mobile", mobile, "operator", operator != null ? operator : "UNKNOWN"));
    }

    // ── NFC Tap & Pay ──

    @PostMapping("/nfc/session")
    public ResponseEntity<NfcPaymentSession> createNfcSession(@RequestBody Map<String, Object> request) {
        UUID userId = UUID.fromString((String) request.get("userId"));
        String merchantVpa = (String) request.get("merchantVpa");
        Long amount = Long.valueOf(request.get("amountPaisa").toString());
        String tagData = (String) request.get("nfcTagData");
        String mode = (String) request.get("cardEmulationMode");
        String terminalId = (String) request.get("terminalId");

        return ResponseEntity.ok(nfcPayService.createSession(userId, merchantVpa, amount, tagData, mode, terminalId));
    }

    @PostMapping("/nfc/{sessionId}/authorize")
    public ResponseEntity<NfcPaymentSession> authorizeNfc(@PathVariable UUID sessionId,
                                                           @RequestParam(defaultValue = "false") boolean pinVerified) {
        return ResponseEntity.ok(nfcPayService.authorize(sessionId, pinVerified));
    }

    @GetMapping("/nfc/tap-go-eligible")
    public ResponseEntity<Map<String, Object>> tapGoEligible(@RequestParam Long amountPaisa) {
        boolean eligible = nfcPayService.isTapAndGoEligible(amountPaisa);
        return ResponseEntity.ok(Map.of("amountPaisa", amountPaisa, "tapAndGoEligible", eligible));
    }

    @PostMapping("/nfc/parse-tag")
    public ResponseEntity<Map<String, String>> parseNfcTag(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(nfcPayService.parseNfcTag(request.get("nfcTagData")));
    }

    // ── IVR / 123Pay ──

    @PostMapping("/ivr/session")
    public ResponseEntity<IvrPaymentSession> createIvrSession(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(ivrPayService.createSession(
                request.get("callerMobile"),
                request.getOrDefault("language", "ne")
        ));
    }

    @PostMapping("/ivr/{sessionId}/dtmf")
    public ResponseEntity<IvrPaymentSession> processDtmf(@PathVariable UUID sessionId,
                                                          @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(ivrPayService.processDtmfInput(sessionId, request.get("dtmfInput")));
    }

    @PostMapping("/ivr/{sessionId}/authorize")
    public ResponseEntity<IvrPaymentSession> authorizeIvr(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(ivrPayService.authorize(sessionId));
    }

    @GetMapping("/ivr/menu/{language}")
    public ResponseEntity<Map<String, String>> getIvrMenu(@PathVariable String language) {
        return ResponseEntity.ok(ivrPayService.getMenuScript(language));
    }

    // ── Credit on UPI ──

    @PostMapping("/credit-upi/link")
    public ResponseEntity<CreditOnUpiCard> linkCreditCard(@RequestBody Map<String, Object> request) {
        UUID userId = UUID.fromString((String) request.get("userId"));
        Long limit = Long.valueOf(request.get("creditLimitPaisa").toString());

        return ResponseEntity.ok(creditOnUpiService.linkCard(
                userId,
                (String) request.get("cardIssuer"),
                (String) request.get("cardLastFour"),
                (String) request.get("cardNetwork"),
                (String) request.get("linkedVpa"),
                limit
        ));
    }

    @DeleteMapping("/credit-upi/{cardId}")
    public ResponseEntity<Void> delinkCreditCard(@PathVariable UUID cardId) {
        creditOnUpiService.delinkCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/credit-upi/cards/{userId}")
    public ResponseEntity<List<CreditOnUpiCard>> getCreditCards(@PathVariable UUID userId) {
        return ResponseEntity.ok(creditOnUpiService.getActiveCards(userId));
    }

    @GetMapping("/credit-upi/{cardId}/limit")
    public ResponseEntity<Map<String, Object>> checkCreditLimit(@PathVariable UUID cardId,
                                                                 @RequestParam Long amountPaisa) {
        boolean hasLimit = creditOnUpiService.hasAvailableLimit(cardId, amountPaisa);
        return ResponseEntity.ok(Map.of("cardId", cardId, "amountPaisa", amountPaisa, "hasAvailableLimit", hasLimit));
    }

    // ── Soundbox ──

    @PostMapping("/soundbox/register")
    public ResponseEntity<SoundboxDevice> registerSoundbox(@RequestBody Map<String, String> request) {
        UUID merchantId = UUID.fromString(request.get("merchantId"));
        return ResponseEntity.ok(soundboxService.registerDevice(
                merchantId,
                request.get("deviceSerial"),
                request.get("deviceModel"),
                request.get("firmwareVersion"),
                request.get("simNumber")
        ));
    }

    @GetMapping("/soundbox/merchant/{merchantId}")
    public ResponseEntity<List<SoundboxDevice>> getMerchantSoundboxes(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(soundboxService.getMerchantDevices(merchantId));
    }

    @PutMapping("/soundbox/{deviceId}/settings")
    public ResponseEntity<SoundboxDevice> updateSoundboxSettings(@PathVariable UUID deviceId,
                                                                   @RequestParam(required = false) String language,
                                                                   @RequestParam(required = false) Integer volume) {
        return ResponseEntity.ok(soundboxService.updateSettings(deviceId, language, volume));
    }

    @PostMapping("/soundbox/{serial}/heartbeat")
    public ResponseEntity<SoundboxDevice> soundboxHeartbeat(@PathVariable String serial) {
        return ResponseEntity.ok(soundboxService.heartbeat(serial));
    }

    @GetMapping("/soundbox/{serial}/firmware")
    public ResponseEntity<Map<String, Object>> checkFirmware(@PathVariable String serial) {
        return ResponseEntity.ok(soundboxService.checkFirmwareUpdate(serial));
    }

    // ── Merchant Refunds ──

    @PostMapping("/refund/full")
    public ResponseEntity<Map<String, Object>> fullRefund(@RequestBody Map<String, String> request) {
        UUID txnId = UUID.fromString(request.get("transactionId"));
        return ResponseEntity.ok(merchantRefundService.initiateFullRefund(
                txnId, request.get("merchantVpa"), request.get("reason")));
    }

    @PostMapping("/refund/partial")
    public ResponseEntity<Map<String, Object>> partialRefund(@RequestBody Map<String, Object> request) {
        UUID txnId = UUID.fromString((String) request.get("transactionId"));
        Long refundAmount = Long.valueOf(request.get("refundAmountPaisa").toString());
        return ResponseEntity.ok(merchantRefundService.initiatePartialRefund(
                txnId, (String) request.get("merchantVpa"), refundAmount, (String) request.get("reason")));
    }

    @GetMapping("/refund/eligibility/{transactionId}")
    public ResponseEntity<Map<String, Object>> refundEligibility(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(merchantRefundService.checkRefundEligibility(transactionId));
    }
}
