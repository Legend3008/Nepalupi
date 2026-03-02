package np.com.nepalupi;

import np.com.nepalupi.domain.dto.response.VpaDetails;
import np.com.nepalupi.domain.entity.BankAccount;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.exception.InvalidVpaException;
import np.com.nepalupi.exception.VpaNotFoundException;
import np.com.nepalupi.repository.BankAccountRepository;
import np.com.nepalupi.repository.VpaRepository;
import np.com.nepalupi.service.vpa.VpaResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VPA Resolution Service Tests")
class VpaResolutionServiceTest {

    @Mock private VpaRepository vpaRepository;
    @Mock private BankAccountRepository bankAccountRepository;

    private VpaResolutionService vpaService;

    @BeforeEach
    void setUp() {
        vpaService = new VpaResolutionService(vpaRepository, bankAccountRepository);
    }

    @Test
    @DisplayName("Valid VPA resolves to account details")
    void validVpaResolvesCorrectly() {
        String vpaAddress = "ritesh@nchl";
        UUID userId = UUID.randomUUID();
        UUID bankAccountId = UUID.randomUUID();

        Vpa vpa = new Vpa();
        vpa.setVpaAddress(vpaAddress);
        vpa.setBankCode("NCHL");
        vpa.setUserId(userId);
        vpa.setBankAccountId(bankAccountId);
        vpa.setIsActive(true);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setId(bankAccountId);
        bankAccount.setAccountNumber("9801234567890");
        bankAccount.setAccountHolder("Ritesh Sharma");

        when(vpaRepository.findByVpaAddressAndIsActiveTrue(vpaAddress)).thenReturn(Optional.of(vpa));
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.of(bankAccount));

        VpaDetails result = vpaService.resolve(vpaAddress);

        assertNotNull(result);
        assertEquals("ritesh@nchl", result.getVpaAddress());
        assertEquals("NCHL", result.getBankCode());
        assertEquals("9801234567890", result.getAccountNumber());
        assertEquals("Ritesh Sharma", result.getAccountHolderName());
        assertEquals(userId, result.getUserId());
        assertTrue(result.isActive());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "no-at-sign", "@nchl", "user@", "user@bank space", "user@@bank"})
    @DisplayName("Invalid VPA format throws InvalidVpaException")
    void invalidVpaFormatThrows(String invalidVpa) {
        assertThrows(InvalidVpaException.class, () -> vpaService.resolve(invalidVpa),
                "Should throw InvalidVpaException for: " + invalidVpa);
    }

    @Test
    @DisplayName("Non-existent VPA throws VpaNotFoundException")
    void nonExistentVpaThrows() {
        String vpa = "nonexistent@nchl";
        when(vpaRepository.findByVpaAddressAndIsActiveTrue(vpa)).thenReturn(Optional.empty());

        assertThrows(VpaNotFoundException.class, () -> vpaService.resolve(vpa));
    }

    @Test
    @DisplayName("VPA with missing bank account throws VpaNotFoundException")
    void missingBankAccountThrows() {
        String vpa = "user@nchl";
        UUID bankAccountId = UUID.randomUUID();

        Vpa vpaEntity = new Vpa();
        vpaEntity.setVpaAddress(vpa);
        vpaEntity.setBankAccountId(bankAccountId);
        vpaEntity.setIsActive(true);

        when(vpaRepository.findByVpaAddressAndIsActiveTrue(vpa)).thenReturn(Optional.of(vpaEntity));
        when(bankAccountRepository.findById(bankAccountId)).thenReturn(Optional.empty());

        assertThrows(VpaNotFoundException.class, () -> vpaService.resolve(vpa));
    }

    @Test
    @DisplayName("VPA with dots and hyphens is valid format")
    void vpaWithDotsAndHyphensIsValid() {
        String vpa = "first.last-name@bank";

        Vpa vpaEntity = new Vpa();
        vpaEntity.setVpaAddress(vpa);
        vpaEntity.setBankCode("BANK");
        vpaEntity.setUserId(UUID.randomUUID());
        vpaEntity.setBankAccountId(UUID.randomUUID());
        vpaEntity.setIsActive(true);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setAccountNumber("1234567890");
        bankAccount.setAccountHolder("Test User");

        when(vpaRepository.findByVpaAddressAndIsActiveTrue(vpa)).thenReturn(Optional.of(vpaEntity));
        when(bankAccountRepository.findById(vpaEntity.getBankAccountId())).thenReturn(Optional.of(bankAccount));

        VpaDetails result = vpaService.resolve(vpa);
        assertNotNull(result);
        assertEquals(vpa, result.getVpaAddress());
    }
}
