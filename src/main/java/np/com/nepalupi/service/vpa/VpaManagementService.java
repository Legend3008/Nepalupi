package np.com.nepalupi.service.vpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.domain.entity.VpaTransferLog;
import np.com.nepalupi.repository.VpaRepository;
import np.com.nepalupi.repository.VpaTransferLogRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Section 4: VPA management — validation, transfer, update linked bank account.
 * <p>
 * VPA Format: username@psphandle (e.g., ritesh@nchl, 9841000001@nabil)
 * PSP handle registered with Nepal UPI (e.g., @nchl = NCHL, @nabil = Nabil Bank)
 * One user can have multiple VPAs across multiple PSPs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VpaManagementService {

    private final VpaRepository vpaRepository;
    private final VpaTransferLogRepository transferLogRepository;
    private final VpaResolutionService vpaResolutionService;

    // Valid Nepal UPI PSP handles
    private static final Set<String> VALID_PSP_HANDLES = Set.of(
            "nchl", "nabil", "global", "nic", "sanima", "mega",
            "nmb", "sunrise", "laxmi", "sbi", "machhapuchhre",
            "kumari", "nepal", "everest", "himalayan", "citizens",
            "prime", "century", "siddhartha", "bok"
    );

    /**
     * Validate VPA format and PSP handle.
     */
    public Map<String, Object> validateVpa(String vpaAddress) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vpa", vpaAddress);

        // Format check: username@handle
        if (!vpaAddress.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$")) {
            result.put("valid", false);
            result.put("error", "Invalid VPA format. Must be username@psphandle");
            return result;
        }

        String[] parts = vpaAddress.split("@");
        String username = parts[0];
        String pspHandle = parts[1].toLowerCase();

        // Username length check
        if (username.length() < 3 || username.length() > 50) {
            result.put("valid", false);
            result.put("error", "Username must be 3-50 characters");
            return result;
        }

        // PSP handle validation
        if (!VALID_PSP_HANDLES.contains(pspHandle)) {
            result.put("valid", false);
            result.put("error", "Unknown PSP handle: @" + pspHandle + ". Valid handles: " + VALID_PSP_HANDLES);
            return result;
        }

        // Availability check
        boolean exists = vpaRepository.existsByVpaAddress(vpaAddress.toLowerCase());

        result.put("valid", true);
        result.put("pspHandle", pspHandle);
        result.put("available", !exists);
        return result;
    }

    /**
     * Transfer VPA to a different bank account (same user).
     * This changes the bank account linked to the VPA.
     */
    @Transactional
    @CacheEvict(value = "vpa-cache", key = "#vpaAddress")
    public Vpa transferVpa(String vpaAddress, UUID newBankAccountId, String newBankCode, UUID userId) {
        Vpa vpa = vpaRepository.findByVpaAddressAndIsActiveTrue(vpaAddress)
                .orElseThrow(() -> new IllegalArgumentException("VPA not found or inactive: " + vpaAddress));

        // Verify ownership
        if (!vpa.getUserId().equals(userId)) {
            throw new IllegalArgumentException("VPA does not belong to this user");
        }

        // Log the transfer
        VpaTransferLog transferLog = VpaTransferLog.builder()
                .vpaAddress(vpaAddress)
                .fromBankCode(vpa.getBankCode())
                .toBankCode(newBankCode)
                .fromAccountId(vpa.getBankAccountId())
                .toAccountId(newBankAccountId)
                .transferredBy(userId)
                .build();
        transferLogRepository.save(transferLog);

        // Update VPA
        vpa.setBankAccountId(newBankAccountId);
        vpa.setBankCode(newBankCode);
        vpa = vpaRepository.save(vpa);

        // Invalidate cache
        vpaResolutionService.invalidateCache(vpaAddress);

        log.info("VPA transferred: vpa={}, from={} to={}", vpaAddress, transferLog.getFromBankCode(), newBankCode);
        return vpa;
    }

    /**
     * Update linked bank account for a VPA (without full transfer logging).
     */
    @Transactional
    @CacheEvict(value = "vpa-cache", key = "#vpaAddress")
    public Vpa updateLinkedAccount(String vpaAddress, UUID newBankAccountId, UUID userId) {
        Vpa vpa = vpaRepository.findByVpaAddressAndIsActiveTrue(vpaAddress)
                .orElseThrow(() -> new IllegalArgumentException("VPA not found: " + vpaAddress));

        if (!vpa.getUserId().equals(userId)) {
            throw new IllegalArgumentException("VPA does not belong to this user");
        }

        vpa.setBankAccountId(newBankAccountId);
        vpa = vpaRepository.save(vpa);

        vpaResolutionService.invalidateCache(vpaAddress);
        log.info("VPA linked account updated: vpa={}, newAccount={}", vpaAddress, newBankAccountId);
        return vpa;
    }

    /**
     * Set a VPA as primary for a user.
     */
    @Transactional
    public Vpa setPrimary(String vpaAddress, UUID userId) {
        // Unset all existing primary VPAs for this user
        List<Vpa> userVpas = vpaRepository.findByUserIdAndIsActiveTrue(userId);
        for (Vpa v : userVpas) {
            if (v.getIsPrimary()) {
                v.setIsPrimary(false);
                vpaRepository.save(v);
            }
        }

        // Set the requested VPA as primary
        Vpa vpa = vpaRepository.findByVpaAddressAndIsActiveTrue(vpaAddress)
                .orElseThrow(() -> new IllegalArgumentException("VPA not found: " + vpaAddress));

        if (!vpa.getUserId().equals(userId)) {
            throw new IllegalArgumentException("VPA does not belong to this user");
        }

        vpa.setIsPrimary(true);
        return vpaRepository.save(vpa);
    }

    /**
     * Suggest VPA addresses for a user.
     */
    public List<String> suggestVpas(String userName, String pspHandle) {
        String base = userName.toLowerCase().replaceAll("[^a-z0-9]", "");
        List<String> suggestions = new ArrayList<>();

        suggestions.add(base + "@" + pspHandle);
        suggestions.add(base + "1@" + pspHandle);
        suggestions.add(base + ".upi@" + pspHandle);
        suggestions.add(base + ".pay@" + pspHandle);

        // Filter out taken ones
        return suggestions.stream()
                .filter(s -> !vpaRepository.existsByVpaAddress(s))
                .toList();
    }
}
