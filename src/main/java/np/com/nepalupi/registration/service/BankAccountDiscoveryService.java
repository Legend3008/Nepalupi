package np.com.nepalupi.registration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.registration.dto.BankAccountDiscoveryResponse;
import np.com.nepalupi.registration.dto.BankAccountDiscoveryResponse.DiscoveredAccount;
import np.com.nepalupi.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bank Account Discovery — Step 2 of UPI registration.
 * <p>
 * In Indian UPI, the PSP app queries the switch, which fans out to all
 * participating banks asking "does this mobile number have accounts here?"
 * Each bank responds with masked account details.
 * <p>
 * Nepal adaptation: Queries NCHL-connected banks via their APIs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountDiscoveryService {

    private final BankAccountRepository bankAccountRepository;

    // Simulated list of participating banks
    private static final List<String> PARTICIPATING_BANKS = List.of(
            "NABIL", "GBIME", "NICA", "SANIMA", "NMB", "MEGA", "PCBL", "HBL", "SBL", "ADBL"
    );

    /**
     * Discover bank accounts linked to the given mobile number.
     * In production, this fans out HTTP/ISO 8583 requests to each bank.
     */
    public BankAccountDiscoveryResponse discoverAccounts(String mobileNumber) {
        log.info("Discovering bank accounts for mobile={}", mobileNumber);

        List<DiscoveredAccount> discovered = new ArrayList<>();

        for (String bankCode : PARTICIPATING_BANKS) {
            // In production: call bank API / ISO 8583 message
            // Simulated: check if any existing accounts match
            List<BankAccount> accounts = bankAccountRepository
                    .findByUserMobileAndBankCode(mobileNumber, bankCode);

            for (BankAccount acc : accounts) {
                discovered.add(DiscoveredAccount.builder()
                        .bankCode(bankCode)
                        .bankName(getBankName(bankCode))
                        .maskedAccountNumber(maskAccountNumber(acc.getAccountNumber()))
                        .accountType(acc.getAccountType())
                        .ifscAvailable(true)
                        .build());
            }
        }

        log.info("Found {} accounts for mobile={}", discovered.size(), mobileNumber);

        return BankAccountDiscoveryResponse.builder()
                .mobileNumber(mobileNumber)
                .accounts(discovered)
                .build();
    }

    /**
     * Link a discovered bank account to the user.
     */
    public BankAccount linkAccount(UUID userId, String bankCode, String accountNumber) {
        log.info("Linking account bank={} acc=****{} to user={}",
                bankCode, accountNumber.substring(Math.max(0, accountNumber.length() - 4)), userId);

        BankAccount account = bankAccountRepository
                .findByBankCodeAndAccountNumber(bankCode, accountNumber)
                .orElseGet(() -> {
                    BankAccount newAcc = BankAccount.builder()
                            .userId(userId)
                            .bankCode(bankCode)
                            .accountNumber(accountNumber)
                            .accountHolder("")
                            .accountType("SAVINGS")
                            .isVerified(false)
                            .isPrimary(true)
                            .build();
                    return bankAccountRepository.save(newAcc);
                });

        account.setUserId(userId);
        return bankAccountRepository.save(account);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private String getBankName(String bankCode) {
        return switch (bankCode) {
            case "NABIL" -> "Nabil Bank";
            case "GBIME" -> "Global IME Bank";
            case "NICA" -> "NIC Asia Bank";
            case "SANIMA" -> "Sanima Bank";
            case "NMB" -> "NMB Bank";
            case "MEGA" -> "Mega Bank";
            case "PCBL" -> "Prime Commercial Bank";
            case "HBL" -> "Himalayan Bank";
            case "SBL" -> "Siddhartha Bank";
            case "ADBL" -> "Agriculture Development Bank";
            default -> bankCode;
        };
    }
}
