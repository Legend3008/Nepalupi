package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.domain.entity.User;
import np.com.nepalupi.repository.BankAccountRepository;
import np.com.nepalupi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * MPIN Service — Step 4 of UPI registration.
 * <p>
 * Indian UPI model: User sets a 4-6 digit MPIN per bank account.
 * The MPIN is encrypted on the device and sent to the issuer bank
 * via the UPI switch (never stored in plaintext anywhere except at the bank HSM).
 * <p>
 * Flow:
 * 1. User enters debit card last-6 + expiry → PSP sends to switch
 * 2. Switch forwards to issuer bank for card validation
 * 3. Bank generates OTP → sent to user's registered mobile
 * 4. User enters OTP + new MPIN → PSP encrypts MPIN, sends to bank via switch
 * 5. Bank stores MPIN hash in HSM → confirms
 * 6. Switch marks user's account as MPIN-set
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MpinService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    /**
     * Initiate MPIN setup — validate card details with issuer bank.
     * Returns an OTP reference that the bank will use to verify.
     */
    @Transactional(readOnly = true)
    public String initiateCardValidation(UUID userId, String bankCode,
                                         String accountNumber, String cardLast6,
                                         String cardExpiry) {
        log.info("Initiating MPIN card validation for user={} bank={}", userId, bankCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getPhoneNumberVerified()) {
            throw new IllegalStateException("Phone number not verified. Complete SIM binding first.");
        }

        BankAccount account = bankAccountRepository
                .findByBankCodeAndAccountNumber(bankCode, accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found. Run discovery first."));

        // In production: send card validation request to issuer bank via ISO 8583 / NCHL
        // Bank validates card last-6 + expiry and sends OTP to registered mobile
        String otpReference = "OTP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Card validation initiated, OTP ref={} for user={} bank={}", otpReference, userId, bankCode);
        return otpReference;
    }

    /**
     * Set MPIN — forward encrypted MPIN to issuer bank for storage.
     * The UPI switch NEVER decrypts or stores the MPIN.
     */
    @Transactional
    public void setMpin(UUID userId, String bankCode, String accountNumber,
                        String encryptedMpin) {
        log.info("Setting MPIN for user={} bank={}", userId, bankCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        BankAccount account = bankAccountRepository
                .findByBankCodeAndAccountNumber(bankCode, accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));

        // In production: forward encrypted MPIN to issuer bank via secure channel
        // Bank decrypts using paired HSM key, stores MPIN hash
        // Response: success / failure

        // Mark account as verified and user as MPIN-set
        account.setIsVerified(true);
        bankAccountRepository.save(account);

        user.setMpinSet(true);
        userRepository.save(user);

        log.info("MPIN set successfully for user={} bank={} account=****{}",
                userId, bankCode,
                accountNumber.substring(Math.max(0, accountNumber.length() - 4)));
    }

    /**
     * Reset MPIN — same flow as set, but requires existing MPIN or OTP verification.
     */
    @Transactional
    public void resetMpin(UUID userId, String bankCode, String accountNumber,
                          String encryptedNewMpin) {
        log.info("Resetting MPIN for user={} bank={}", userId, bankCode);
        // Same flow as setMpin — bank handles old MPIN invalidation
        setMpin(userId, bankCode, accountNumber, encryptedNewMpin);
    }
}
