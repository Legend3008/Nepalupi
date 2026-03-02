package np.com.nepalupi;

import np.com.nepalupi.service.psp.PspCredentialService;
import np.com.nepalupi.domain.entity.Psp;
import np.com.nepalupi.repository.PspRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PSP Credential Service Tests")
class PspCredentialServiceTest {

    @Mock private PspRepository pspRepository;

    private PspCredentialService credentialService;

    @BeforeEach
    void setUp() {
        credentialService = new PspCredentialService(pspRepository);
    }

    @Test
    @DisplayName("Sandbox token starts with 'sandbox_'")
    void sandboxTokenFormat() {
        String token = credentialService.generateSandboxToken(UUID.randomUUID());

        assertNotNull(token);
        assertTrue(token.startsWith("sbx_"), "Sandbox token should start with 'sbx_'");
        assertTrue(token.length() > 20, "Sandbox token should be reasonably long");
    }

    @Test
    @DisplayName("Sandbox tokens are unique each call")
    void sandboxTokensUnique() {
        UUID pspId = UUID.randomUUID();
        String t1 = credentialService.generateSandboxToken(pspId);
        String t2 = credentialService.generateSandboxToken(pspId);

        assertNotEquals(t1, t2, "Each sandbox token call should generate a unique token");
    }

    @Test
    @DisplayName("Production credentials contain API key, secret, and webhook secret")
    void productionCredentialsComplete() {
        Psp psp = new Psp();
        psp.setId(UUID.randomUUID());
        psp.setPspId("TESTPSP001");

        when(pspRepository.save(any(Psp.class))).thenReturn(psp);

        PspCredentialService.ProductionCredentials creds =
                credentialService.generateProductionCredentials(psp);

        assertNotNull(creds.apiKey());
        assertNotNull(creds.secret());
        assertNotNull(creds.webhookSigningSecret());
        assertTrue(creds.apiKey().startsWith("nupi_"),
                "API key should start with 'nupi_'");
    }

    @Test
    @DisplayName("Rotate API key generates a new key")
    void rotateApiKeyGeneratesNew() {
        UUID pspId = UUID.randomUUID();
        Psp psp = new Psp();
        psp.setId(pspId);
        psp.setPspId("TESTPSP002");
        psp.setApiKeyHash("old-hash");

        when(pspRepository.findById(pspId)).thenReturn(Optional.of(psp));
        when(pspRepository.save(any(Psp.class))).thenReturn(psp);

        PspCredentialService.ProductionCredentials creds =
                credentialService.rotateApiKey(pspId);

        assertNotNull(creds.apiKey());
        assertTrue(creds.apiKey().startsWith("nupi_"));
        verify(pspRepository).save(psp);
    }

    @Test
    @DisplayName("Verify credential matches BCrypt hash")
    void verifyCredentialMatches() {
        // Generate a credential, then verify against its hash
        Psp psp = new Psp();
        psp.setId(UUID.randomUUID());
        psp.setPspId("TESTPSP003");

        when(pspRepository.save(any(Psp.class))).thenReturn(psp);

        PspCredentialService.ProductionCredentials creds =
                credentialService.generateProductionCredentials(psp);

        // The PSP should have the hash stored after save
        String apiKeyHash = psp.getApiKeyHash();
        assertNotNull(apiKeyHash, "Hash should be stored on PSP entity");

        // Verify the raw API key against the stored hash
        assertTrue(credentialService.verifyCredential(creds.apiKey(), apiKeyHash),
                "Raw API key should verify against its BCrypt hash");
    }

    @Test
    @DisplayName("Wrong credential does not verify")
    void wrongCredentialDoesNotVerify() {
        Psp psp = new Psp();
        psp.setId(UUID.randomUUID());
        psp.setPspId("TESTPSP004");

        when(pspRepository.save(any(Psp.class))).thenReturn(psp);

        credentialService.generateProductionCredentials(psp);

        assertFalse(credentialService.verifyCredential("wrong_key", psp.getApiKeyHash()),
                "Wrong key should not verify");
    }

    @Test
    @DisplayName("Rotate webhook secret generates new secret")
    void rotateWebhookSecret() {
        UUID pspId = UUID.randomUUID();
        Psp psp = new Psp();
        psp.setId(pspId);
        psp.setPspId("TESTPSP005");
        psp.setWebhookSigningSecret("old-webhook-hash");

        when(pspRepository.findById(pspId)).thenReturn(Optional.of(psp));
        when(pspRepository.save(any(Psp.class))).thenReturn(psp);

        String newSecret = credentialService.rotateWebhookSecret(pspId);

        assertNotNull(newSecret);
        assertTrue(newSecret.startsWith("whsec_"),
                "Webhook secret should start with 'whsec_'");
        verify(pspRepository).save(psp);
    }
}
