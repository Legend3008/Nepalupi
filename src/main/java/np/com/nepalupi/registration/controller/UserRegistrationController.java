package np.com.nepalupi.registration.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.registration.dto.*;
import np.com.nepalupi.registration.entity.DeviceChangeRequest;
import np.com.nepalupi.registration.entity.UserKyc;
import np.com.nepalupi.registration.service.DeviceChangeService;
import np.com.nepalupi.registration.service.KycService;
import np.com.nepalupi.registration.service.UserRegistrationOrchestrator;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * User Registration & KYC Controller — Module 9.
 * <p>
 * Exposes the complete UPI registration flow as REST APIs
 * matching the Indian UPI registration lifecycle:
 * <p>
 * POST /api/v1/registration/initiate          → Step 1: SIM binding
 * POST /api/v1/registration/verify-sms        → Step 1b: SMS verification
 * GET  /api/v1/registration/discover-accounts → Step 2: Bank account discovery
 * POST /api/v1/registration/link-account      → Step 2b: Link account
 * POST /api/v1/registration/create-vpa        → Step 3: VPA creation
 * POST /api/v1/registration/mpin/initiate     → Step 4a: Card validation
 * POST /api/v1/registration/mpin/set          → Step 4b: Set MPIN
 * POST /api/v1/registration/kyc/minimum       → Step 5: Minimum KYC
 * POST /api/v1/registration/kyc/full          → Step 6: Full KYC
 * GET  /api/v1/registration/status/{userId}   → Check status
 * POST /api/v1/registration/device-change     → Account recovery
 */
@RestController
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
@Tag(name = "User Registration", description = "SIM binding, bank linking, VPA creation, MPIN & KYC")
public class UserRegistrationController {

    private final UserRegistrationOrchestrator orchestrator;
    private final DeviceChangeService deviceChangeService;
    private final KycService kycService;

    // ── Step 1: SIM Binding ──

    @PostMapping("/initiate")
    public ResponseEntity<RegistrationResponse> initiateRegistration(
            @Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.ok(orchestrator.initiateRegistration(request));
    }

    @PostMapping("/verify-sms")
    public ResponseEntity<RegistrationResponse> verifySmsBinding(
            @RequestParam String bindingSmsId) {
        return ResponseEntity.ok(orchestrator.verifySmsBinding(bindingSmsId));
    }

    // ── Step 2: Bank Account Discovery ──

    @GetMapping("/discover-accounts")
    public ResponseEntity<BankAccountDiscoveryResponse> discoverAccounts(
            @RequestParam String mobileNumber) {
        return ResponseEntity.ok(orchestrator.discoverBankAccounts(mobileNumber));
    }

    @PostMapping("/link-account")
    public ResponseEntity<BankAccount> linkAccount(
            @RequestParam UUID userId,
            @RequestParam String bankCode,
            @RequestParam String accountNumber) {
        return ResponseEntity.ok(orchestrator.linkBankAccount(userId, bankCode, accountNumber));
    }

    // ── Step 3: VPA Creation ──

    @PostMapping("/create-vpa")
    public ResponseEntity<Vpa> createVpa(
            @RequestParam UUID userId,
            @RequestParam String bankCode,
            @RequestParam UUID bankAccountId,
            @RequestParam String desiredVpa) {
        return ResponseEntity.ok(orchestrator.createVpa(userId, bankCode, bankAccountId, desiredVpa));
    }

    // ── Step 4: MPIN Setup ──

    @PostMapping("/mpin/initiate")
    public ResponseEntity<Map<String, String>> initiateMpinSetup(
            @Valid @RequestBody MpinSetupRequest request) {
        String otpRef = orchestrator.initiateMpinSetup(request);
        return ResponseEntity.ok(Map.of("otpReference", otpRef, "message", "OTP sent to registered mobile"));
    }

    @PostMapping("/mpin/set")
    public ResponseEntity<Map<String, String>> setMpin(
            @Valid @RequestBody MpinSetupRequest request) {
        orchestrator.setMpin(request);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "MPIN set successfully"));
    }

    // ── Step 5 & 6: KYC ──

    @PostMapping("/kyc/minimum")
    public ResponseEntity<UserKyc> submitMinimumKyc(
            @Valid @RequestBody KycSubmissionRequest request) {
        return ResponseEntity.ok(orchestrator.submitMinimumKyc(request));
    }

    @PostMapping("/kyc/full")
    public ResponseEntity<UserKyc> submitFullKyc(
            @Valid @RequestBody KycSubmissionRequest request) {
        return ResponseEntity.ok(orchestrator.submitFullKyc(request));
    }

    @PostMapping("/kyc/{kycId}/approve")
    public ResponseEntity<UserKyc> approveKyc(
            @PathVariable UUID kycId,
            @RequestParam String approverName) {
        return ResponseEntity.ok(kycService.approveKyc(kycId, approverName));
    }

    @PostMapping("/kyc/{kycId}/reject")
    public ResponseEntity<UserKyc> rejectKyc(
            @PathVariable UUID kycId,
            @RequestParam String reason,
            @RequestParam String approverName) {
        return ResponseEntity.ok(kycService.rejectKyc(kycId, reason, approverName));
    }

    // ── Registration Status ──

    @GetMapping("/status/{userId}")
    public ResponseEntity<RegistrationResponse> getRegistrationStatus(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(orchestrator.getRegistrationStatus(userId));
    }

    // ── Device Change (Account Recovery) ──

    @PostMapping("/device-change/initiate")
    public ResponseEntity<DeviceChangeRequest> initiateDeviceChange(
            @Valid @RequestBody DeviceChangeRequestDto request) {
        return ResponseEntity.ok(deviceChangeService.initiateDeviceChange(
                request.getUserId(), request.getNewDeviceId(),
                request.getNewSimSerial(), request.getEncryptedMpin()));
    }

    @PostMapping("/device-change/{requestId}/complete")
    public ResponseEntity<DeviceChangeRequest> completeDeviceChange(
            @PathVariable UUID requestId) {
        return ResponseEntity.ok(deviceChangeService.completeDeviceChange(requestId));
    }
}
