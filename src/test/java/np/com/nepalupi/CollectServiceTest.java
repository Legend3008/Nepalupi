package np.com.nepalupi;

import np.com.nepalupi.mandate.entity.CollectRequest;
import np.com.nepalupi.mandate.enums.CollectRequestStatus;
import np.com.nepalupi.mandate.repository.CollectRequestRepository;
import np.com.nepalupi.mandate.service.CollectService;
import np.com.nepalupi.service.transaction.TransactionOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Collect Service Tests")
class CollectServiceTest {

    @Mock private CollectRequestRepository collectRequestRepository;
    @Mock private TransactionOrchestrator transactionOrchestrator;

    private CollectService collectService;

    @BeforeEach
    void setUp() {
        collectService = new CollectService(collectRequestRepository, transactionOrchestrator);
    }

    @Test
    @DisplayName("Create collect request saves with PENDING status")
    void createCollectRequestSavesPending() {
        when(collectRequestRepository.save(any(CollectRequest.class)))
                .thenAnswer(invocation -> {
                    CollectRequest cr = invocation.getArgument(0);
                    cr.setId(UUID.randomUUID());
                    return cr;
                });

        CollectRequest result = collectService.createCollectRequest(
                "merchant@nchl", "payer@nchl", 100000L, "Payment for order #123");

        assertNotNull(result);
        assertEquals(CollectRequestStatus.PENDING, result.getStatus());
        assertEquals("merchant@nchl", result.getRequestorVpa());
        assertEquals("payer@nchl", result.getPayerVpa());
        assertEquals(100000L, result.getAmountPaisa());
        assertNotNull(result.getExpiresAt());

        verify(collectRequestRepository).save(any(CollectRequest.class));
    }

    @Test
    @DisplayName("Cannot send collect request to yourself")
    void cannotSendToSelf() {
        assertThrows(IllegalArgumentException.class,
                () -> collectService.createCollectRequest(
                        "user@nchl", "user@nchl", 5000L, "Self-payment"));
    }

    @Test
    @DisplayName("Reject collect request sets REJECTED status")
    void rejectCollectRequest() {
        UUID requestId = UUID.randomUUID();
        CollectRequest pending = CollectRequest.builder()
                .id(requestId)
                .collectRef("COL-TEST-001")
                .requestorVpa("merchant@nchl")
                .payerVpa("payer@nchl")
                .amountPaisa(50000L)
                .status(CollectRequestStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();

        when(collectRequestRepository.findById(requestId)).thenReturn(Optional.of(pending));
        when(collectRequestRepository.save(any(CollectRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CollectRequest result = collectService.reject(requestId, "Not authorized");

        assertEquals(CollectRequestStatus.REJECTED, result.getStatus());
        assertEquals("Not authorized", result.getRejectionReason());
        assertNotNull(result.getRespondedAt());
    }

    @Test
    @DisplayName("Cannot reject a non-PENDING collect request")
    void cannotRejectNonPending() {
        UUID requestId = UUID.randomUUID();
        CollectRequest approved = CollectRequest.builder()
                .id(requestId)
                .collectRef("COL-TEST-002")
                .status(CollectRequestStatus.APPROVED)
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();

        when(collectRequestRepository.findById(requestId)).thenReturn(Optional.of(approved));

        assertThrows(IllegalStateException.class,
                () -> collectService.reject(requestId, "Too late"));
    }

    @Test
    @DisplayName("Collect request expires at 30 minutes from creation")
    void expiresAfter30Minutes() {
        when(collectRequestRepository.save(any(CollectRequest.class)))
                .thenAnswer(invocation -> {
                    CollectRequest cr = invocation.getArgument(0);
                    cr.setId(UUID.randomUUID());
                    return cr;
                });

        CollectRequest result = collectService.createCollectRequest(
                "payee@nchl", "payer@nchl", 25000L, "Test");

        Instant expectedExpiry = Instant.now().plusSeconds(30 * 60);
        // Allow 5-second tolerance
        assertTrue(Math.abs(result.getExpiresAt().getEpochSecond() - expectedExpiry.getEpochSecond()) < 5,
                "Expiry should be ~30 minutes from now");
    }
}
