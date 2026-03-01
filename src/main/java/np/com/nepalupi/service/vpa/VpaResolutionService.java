package np.com.nepalupi.service.vpa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.dto.response.VpaDetails;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.exception.InvalidVpaException;
import np.com.nepalupi.exception.VpaNotFoundException;
import np.com.nepalupi.repository.BankAccountRepository;
import np.com.nepalupi.repository.VpaRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Resolves a VPA (Virtual Payment Address) like "ritesh@nchl" to
 * the underlying bank code, account number, and holder name.
 * <p>
 * Results are cached in Redis with 5-minute TTL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VpaResolutionService {

    private static final String VPA_REGEX = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9]+$";

    private final VpaRepository vpaRepository;
    private final BankAccountRepository bankAccountRepository;

    /**
     * Resolve a VPA address to full account details.
     *
     * @param vpa the VPA address (e.g., "ritesh@nchl")
     * @return resolved details including bank code and account number
     * @throws InvalidVpaException   if format is invalid
     * @throws VpaNotFoundException  if VPA does not exist or is inactive
     */
    @Cacheable(value = "vpa-cache", key = "#vpa", unless = "#result == null")
    public VpaDetails resolve(String vpa) {
        log.debug("Resolving VPA: {}", vpa);

        if (!vpa.matches(VPA_REGEX)) {
            throw new InvalidVpaException("Invalid VPA format: " + vpa);
        }

        Vpa vpaEntity = vpaRepository.findByVpaAddressAndIsActiveTrue(vpa)
                .orElseThrow(() -> new VpaNotFoundException("VPA not found or inactive: " + vpa));

        BankAccount bankAccount = bankAccountRepository.findById(vpaEntity.getBankAccountId())
                .orElseThrow(() -> new VpaNotFoundException(
                        "Bank account not found for VPA: " + vpa));

        return VpaDetails.builder()
                .vpaAddress(vpaEntity.getVpaAddress())
                .bankCode(vpaEntity.getBankCode())
                .accountNumber(bankAccount.getAccountNumber())
                .accountHolderName(bankAccount.getAccountHolder())
                .userId(vpaEntity.getUserId())
                .bankAccountId(vpaEntity.getBankAccountId())
                .active(vpaEntity.getIsActive())
                .build();
    }

    /**
     * Evict cached VPA details — called when VPA is updated or deactivated.
     */
    @CacheEvict(value = "vpa-cache", key = "#vpa")
    public void invalidateCache(String vpa) {
        log.info("VPA cache invalidated for: {}", vpa);
    }
}
