package np.com.nepalupi;

import com.fasterxml.jackson.databind.ObjectMapper;
import np.com.nepalupi.domain.entity.FraudFlag;
import np.com.nepalupi.domain.entity.Vpa;
import np.com.nepalupi.domain.enums.FraudSignal;
import np.com.nepalupi.repository.FraudFlagRepository;
import np.com.nepalupi.repository.TransactionRepository;
import np.com.nepalupi.repository.VpaRepository;
import np.com.nepalupi.service.fraud.FraudEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fraud Engine Tests")
class FraudEngineTest {

    @Mock private TransactionRepository txnRepo;
    @Mock private FraudFlagRepository fraudFlagRepository;
    @Mock private VpaRepository vpaRepository;

    private FraudEngine fraudEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fraudEngine = new FraudEngine(txnRepo, fraudFlagRepository, vpaRepository, objectMapper);
    }

    @Test
    @DisplayName("No signals when transaction is normal")
    void noSignalsForNormalTransaction() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();

        // Average amount is 1000, transaction is 2000 (< 5x)
        when(txnRepo.getAverageAmount(eq(userId), any(Instant.class))).thenReturn(1000.0);
        // Only 2 recent transactions (< 5 threshold)
        when(txnRepo.countTransactionsSince(eq(userId), any(Instant.class))).thenReturn(2);

        List<FraudSignal> signals = fraudEngine.assess(userId, 2000L, txnId);

        assertTrue(signals.isEmpty(), "Normal transaction should produce no fraud signals");
    }

    @Test
    @DisplayName("AMOUNT_SPIKE detected when amount > 5x average")
    void amountSpikeDetected() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();

        // Average is 1000, transaction is 6000 (> 5x)
        when(txnRepo.getAverageAmount(eq(userId), any(Instant.class))).thenReturn(1000.0);
        when(txnRepo.countTransactionsSince(eq(userId), any(Instant.class))).thenReturn(1);

        List<FraudSignal> signals = fraudEngine.assess(userId, 6000L, txnId);

        assertTrue(signals.contains(FraudSignal.AMOUNT_SPIKE),
                "Should detect AMOUNT_SPIKE when amount > 5x average");
    }

    @Test
    @DisplayName("HIGH_VELOCITY detected when > 5 txns/hour")
    void highVelocityDetected() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();

        when(txnRepo.getAverageAmount(eq(userId), any(Instant.class))).thenReturn(null);
        when(txnRepo.countTransactionsSince(eq(userId), any(Instant.class))).thenReturn(6);

        List<FraudSignal> signals = fraudEngine.assess(userId, 1000L, txnId);

        assertTrue(signals.contains(FraudSignal.HIGH_VELOCITY),
                "Should detect HIGH_VELOCITY when > 5 transactions in last hour");
    }

    @Test
    @DisplayName("NEW_PAYEE_VPA detected for newly registered VPA")
    void newPayeeVpaDetected() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        String payeeVpa = "new-user@nchl";

        when(txnRepo.getAverageAmount(eq(userId), any(Instant.class))).thenReturn(null);
        when(txnRepo.countTransactionsSince(eq(userId), any(Instant.class))).thenReturn(0);

        Vpa vpaEntity = new Vpa();
        vpaEntity.setCreatedAt(Instant.now().minus(12, ChronoUnit.HOURS)); // Created 12hrs ago
        when(vpaRepository.findByVpaAddress(payeeVpa)).thenReturn(Optional.of(vpaEntity));

        List<FraudSignal> signals = fraudEngine.assess(userId, 1000L, txnId, null, payeeVpa);

        assertTrue(signals.contains(FraudSignal.NEW_PAYEE_VPA),
                "Should detect NEW_PAYEE_VPA for VPA created < 24 hours ago");
    }

    @Test
    @DisplayName("Multiple signals trigger fraud flag for review")
    void multipleSignalsTriggerFlag() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();

        // Trigger AMOUNT_SPIKE
        when(txnRepo.getAverageAmount(eq(userId), any(Instant.class))).thenReturn(100.0);
        // Trigger HIGH_VELOCITY
        when(txnRepo.countTransactionsSince(eq(userId), any(Instant.class))).thenReturn(10);

        List<FraudSignal> signals = fraudEngine.assess(userId, 1000L, txnId);

        assertTrue(signals.size() >= 2, "Should detect at least 2 signals");
        // Verify fraud flag saved for review
        verify(fraudFlagRepository).save(any(FraudFlag.class));
    }

    @Test
    @DisplayName("Single signal does not trigger fraud flag save")
    void singleSignalNoFlag() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();

        when(txnRepo.getAverageAmount(eq(userId), any(Instant.class))).thenReturn(100.0);
        when(txnRepo.countTransactionsSince(eq(userId), any(Instant.class))).thenReturn(1);

        List<FraudSignal> signals = fraudEngine.assess(userId, 600L, txnId);

        assertEquals(1, signals.size());
        assertTrue(signals.contains(FraudSignal.AMOUNT_SPIKE));
        verify(fraudFlagRepository, never()).save(any());
    }

    @Test
    @DisplayName("Exception in rule does not crash the engine")
    void exceptionHandledGracefully() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();

        when(txnRepo.getAverageAmount(eq(userId), any(Instant.class)))
                .thenThrow(new RuntimeException("DB error"));
        when(txnRepo.countTransactionsSince(eq(userId), any(Instant.class)))
                .thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> fraudEngine.assess(userId, 1000L, txnId),
                "FraudEngine should handle exceptions gracefully");
    }
}
