package np.com.nepalupi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.repository.BankAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bank Account Management API — manage linked bank accounts.
 * Covers: set primary, remove, refresh, list.
 */
@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "Manage linked bank accounts")
public class BankAccountController {

    private final BankAccountRepository bankAccountRepository;

    @GetMapping("/user/{userId}")
    @Operation(summary = "List linked accounts", description = "Get all bank accounts linked to a user")
    public ResponseEntity<List<BankAccount>> listAccounts(@PathVariable UUID userId) {
        return ResponseEntity.ok(bankAccountRepository.findByUserId(userId));
    }

    @PutMapping("/{accountId}/primary")
    @Operation(summary = "Set primary bank account", description = "Set a bank account as the primary account for UPI transactions")
    public ResponseEntity<Map<String, Object>> setPrimary(
            @PathVariable UUID accountId) {

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + accountId));

        // Clear all existing primaries for this user
        List<BankAccount> userAccounts = bankAccountRepository.findByUserId(account.getUserId());
        userAccounts.forEach(a -> {
            if (a.getIsPrimary()) {
                a.setIsPrimary(false);
                bankAccountRepository.save(a);
            }
        });

        // Set the selected account as primary
        account.setIsPrimary(true);
        bankAccountRepository.save(account);

        return ResponseEntity.ok(Map.of(
                "message", "Primary bank account updated",
                "accountId", accountId,
                "bankCode", account.getBankCode(),
                "maskedAccount", maskAccount(account.getAccountNumber())
        ));
    }

    @DeleteMapping("/{accountId}/unlink")
    @Operation(summary = "Unlink bank account", description = "Remove a linked bank account")
    public ResponseEntity<Map<String, Object>> unlinkAccount(@PathVariable UUID accountId) {

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + accountId));

        if (account.getIsPrimary()) {
            throw new IllegalStateException("Cannot remove primary bank account. Set another account as primary first.");
        }

        // Soft delete — mark as inactive
        account.setIsActive(false);
        bankAccountRepository.save(account);

        return ResponseEntity.ok(Map.of(
                "message", "Bank account unlinked successfully",
                "accountId", accountId
        ));
    }

    @PostMapping("/user/{userId}/refresh")
    @Operation(summary = "Refresh bank accounts", description = "Re-discover and refresh bank account details from issuer banks")
    public ResponseEntity<Map<String, Object>> refreshAccounts(@PathVariable UUID userId) {

        List<BankAccount> accounts = bankAccountRepository.findByUserId(userId);

        // In production: call each bank's API to refresh account details (balance, status, etc.)
        // For now, update the refreshed timestamp
        accounts.forEach(a -> {
            a.setUpdatedAt(java.time.Instant.now());
            bankAccountRepository.save(a);
        });

        return ResponseEntity.ok(Map.of(
                "message", "Bank accounts refreshed",
                "userId", userId,
                "accountCount", accounts.size()
        ));
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Check balance", description = "Check balance of a linked bank account")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID accountId) {

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + accountId));

        // In production: call bank's balance API
        // Mock balance for development
        long mockBalance = 5000000L; // Rs 50,000

        return ResponseEntity.ok(Map.of(
                "accountId", accountId,
                "bankCode", account.getBankCode(),
                "maskedAccount", maskAccount(account.getAccountNumber()),
                "balancePaisa", mockBalance,
                "balanceNPR", mockBalance / 100.0,
                "currency", "NPR"
        ));
    }

    private String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "****";
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
