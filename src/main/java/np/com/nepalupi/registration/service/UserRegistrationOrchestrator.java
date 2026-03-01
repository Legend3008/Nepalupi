package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.registration.dto.*;
import np.com.nepalupi.registration.entity.DeviceBinding;
import np.com.nepalupi.registration.entity.UserKyc;
import np.com.nepalupi.registration.enums.KycLevel;
import np.com.nepalupi.repository.UserRepository;
import np.com.nepalupi.repository.VpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User Registration Orchestrator — coordinates the entire UPI registration flow.
 * <p>
 * Indian UPI registration flow (exactly replicated):
 * <p>
 * Step 1: SIM Binding       → PSP sends silent SMS, phone number verified
 * Step 2: Bank Discovery    → Fan-out to banks, user picks account(s)
 * Step 3: VPA Creation      → user@bankcode format, availability check
 * Step 4: MPIN Setup        → Card last-6 + OTP + encrypted MPIN to bank
 * Step 5: Minimum KYC       → Self-declared info, auto-approved
 * Step 6: (Optional) Full KYC → Document + selfie, higher limits
 * <p>
 * After steps 1-5, user is fully registered and can transact.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationOrchestrator {

    private final SimBindingService simBindingService;
    private final BankAccountDiscoveryService bankDiscoveryService;
    private final MpinService mpinService;
    private final KycService kycService;
    private final UserRepository userRepository;
    private final VpaRepository vpaRepository;

    /**
     * Step 1: Initiate registration — trigger SIM binding.
     */
    @Transactional
    public RegistrationResponse initiateRegistration(RegistrationRequest request) {
        log.info("Starting UPI registration for mobile={}", request.getMobileNumber());

        DeviceBinding binding = simBindingService.initiateSmsBinding(
                request.getMobileNumber(),
                request.getDeviceId(),
                request.getImei(),
                request.getSimSerial()
        );

        return RegistrationResponse.builder()
                .mobileNumber(request.getMobileNumber())
                .status("DEVICE_BINDING_PENDING")
                .message("Silent SMS sent. Waiting for verification.")
                .bindingSmsId(binding.getBindingSmsId())
                .build();
    }

    /**
     * Step 1b: Verify SIM binding (called when switch receives the SMS).
     */
    @Transactional
    public RegistrationResponse verifySmsBinding(String bindingSmsId) {
        DeviceBinding binding = simBindingService.verifyBinding(bindingSmsId);

        return RegistrationResponse.builder()
                .userId(binding.getUserId())
                .mobileNumber(binding.getMobileNumber())
                .status("SIM_VERIFIED")
                .message("Phone number verified. Proceed to bank account discovery.")
                .build();
    }

    /**
     * Step 2: Discover bank accounts.
     */
    public BankAccountDiscoveryResponse discoverBankAccounts(String mobileNumber) {
        return bankDiscoveryService.discoverAccounts(mobileNumber);
    }

    /**
     * Step 2b: Link a selected bank account.
     */
    @Transactional
    public BankAccount linkBankAccount(UUID userId, String bankCode, String accountNumber) {
        return bankDiscoveryService.linkAccount(userId, bankCode, accountNumber);
    }

    /**
     * Step 3: Create VPA (Virtual Payment Address).
     * Format: username@bankcode (e.g., ritesh@nabil)
     */
    @Transactional
    public Vpa createVpa(UUID userId, String bankCode, UUID bankAccountId, String desiredVpa) {
        log.info("Creating VPA={} for user={}", desiredVpa, userId);

        // Check availability
        if (vpaRepository.existsByVpaAddress(desiredVpa)) {
            throw new IllegalStateException("VPA '" + desiredVpa + "' is already taken");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Vpa vpa = Vpa.builder()
                .vpaAddress(desiredVpa)
                .userId(userId)
                .bankAccountId(bankAccountId)
                .bankCode(bankCode)
                .isPrimary(true)
                .isActive(true)
                .build();

        vpa = vpaRepository.save(vpa);
        log.info("VPA={} created for user={}", desiredVpa, userId);
        return vpa;
    }

    /**
     * Step 4a: Initiate MPIN setup (card validation).
     */
    public String initiateMpinSetup(MpinSetupRequest request) {
        return mpinService.initiateCardValidation(
                request.getUserId(),
                request.getBankCode(),
                request.getAccountNumber(),
                request.getCardLast6Digits(),
                request.getCardExpiry()
        );
    }

    /**
     * Step 4b: Set MPIN.
     */
    @Transactional
    public void setMpin(MpinSetupRequest request) {
        mpinService.setMpin(
                request.getUserId(),
                request.getBankCode(),
                request.getAccountNumber(),
                request.getEncryptedMpin()
        );
    }

    /**
     * Step 5: Submit Minimum KYC.
     */
    @Transactional
    public UserKyc submitMinimumKyc(KycSubmissionRequest request) {
        return kycService.submitMinimumKyc(request);
    }

    /**
     * Step 6: Submit Full KYC (optional, for higher limits).
     */
    @Transactional
    public UserKyc submitFullKyc(KycSubmissionRequest request) {
        return kycService.submitFullKyc(request);
    }

    /**
     * Check registration status for a user.
     */
    @Transactional(readOnly = true)
    public RegistrationResponse getRegistrationStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String status;
        String message;

        if (!user.getPhoneNumberVerified()) {
            status = "PENDING_SIM_BINDING";
            message = "Complete SIM binding";
        } else if (!user.getMpinSet()) {
            status = "PENDING_MPIN";
            message = "Discover bank account and set MPIN";
        } else if ("NONE".equals(user.getKycLevel())) {
            status = "PENDING_KYC";
            message = "Complete minimum KYC";
        } else {
            status = "REGISTERED";
            message = "Registration complete. KYC level: " + user.getKycLevel();
        }

        return RegistrationResponse.builder()
                .userId(userId)
                .mobileNumber(user.getMobileNumber())
                .status(status)
                .message(message)
                .build();
    }
}
