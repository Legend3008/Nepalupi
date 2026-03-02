package np.com.nepalupi;

import np.com.nepalupi.mandate.entity.Mandate;
import np.com.nepalupi.mandate.enums.MandateFrequency;
import np.com.nepalupi.mandate.enums.MandateStatus;
import np.com.nepalupi.mandate.repository.MandateRepository;
import np.com.nepalupi.mandate.service.MandateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mandate Service Tests")
class MandateServiceTest {

    @Mock private MandateRepository mandateRepository;

    private MandateService mandateService;

    @BeforeEach
    void setUp() {
        mandateService = new MandateService(mandateRepository);
    }

    @Test
    @DisplayName("Create recurring mandate with PENDING_APPROVAL status")
    void createRecurringMandate() {
        when(mandateRepository.save(any(Mandate.class)))
                .thenAnswer(invocation -> {
                    Mandate m = invocation.getArgument(0);
                    m.setId(UUID.randomUUID());
                    return m;
                });

        Mandate result = mandateService.createMandate(
                "merchant@nchl", "payer@nchl",
                100000L, 200000L,
                MandateFrequency.MONTHLY, "SUBSCRIPTION",
                "Netflix subscription", LocalDate.now(), LocalDate.now().plusYears(1),
                false);

        assertEquals(MandateStatus.PENDING_APPROVAL, result.getStatus());
        assertEquals("RECURRING", result.getMandateType());
        assertEquals("merchant@nchl", result.getMerchantVpa());
        assertEquals("payer@nchl", result.getPayerVpa());
        assertEquals(100000L, result.getAmountPaisa());
        assertEquals(200000L, result.getMaxAmountPaisa());
        assertEquals(0, result.getCoolingPeriodMinutes());
    }

    @Test
    @DisplayName("One-time mandate has 12-hour cooling period")
    void oneTimeMandateHasCoolingPeriod() {
        when(mandateRepository.save(any(Mandate.class)))
                .thenAnswer(invocation -> {
                    Mandate m = invocation.getArgument(0);
                    m.setId(UUID.randomUUID());
                    return m;
                });

        Mandate result = mandateService.createMandate(
                "merchant@nchl", "payer@nchl",
                500000L, 500000L,
                MandateFrequency.ONE_TIME, "INSURANCE",
                "Insurance premium", LocalDate.now().plusDays(7), null,
                true);

        assertEquals("ONE_TIME", result.getMandateType());
        assertEquals(720, result.getCoolingPeriodMinutes()); // 12 hours = 720 minutes
    }

    @Test
    @DisplayName("Approve mandate transitions to ACTIVE")
    void approveMandateTransitionsToActive() {
        UUID mandateId = UUID.randomUUID();
        Mandate pending = Mandate.builder()
                .id(mandateId)
                .mandateRef("MND-TEST-001")
                .status(MandateStatus.PENDING_APPROVAL)
                .mandateType("RECURRING")
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(pending));
        when(mandateRepository.save(any(Mandate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mandate result = mandateService.approve(mandateId);

        assertEquals(MandateStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getApprovedAt());
    }

    @Test
    @DisplayName("Cannot approve already active mandate")
    void cannotApproveAlreadyActive() {
        UUID mandateId = UUID.randomUUID();
        Mandate active = Mandate.builder()
                .id(mandateId)
                .status(MandateStatus.ACTIVE)
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(active));

        assertThrows(IllegalStateException.class,
                () -> mandateService.approve(mandateId));
    }

    @Test
    @DisplayName("Pause active mandate transitions to PAUSED")
    void pauseActiveMandate() {
        UUID mandateId = UUID.randomUUID();
        Mandate active = Mandate.builder()
                .id(mandateId)
                .mandateRef("MND-TEST-002")
                .status(MandateStatus.ACTIVE)
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(active));
        when(mandateRepository.save(any(Mandate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mandate result = mandateService.pause(mandateId);

        assertEquals(MandateStatus.PAUSED, result.getStatus());
        assertNotNull(result.getPausedAt());
    }

    @Test
    @DisplayName("Resume paused mandate transitions back to ACTIVE")
    void resumePausedMandate() {
        UUID mandateId = UUID.randomUUID();
        Mandate paused = Mandate.builder()
                .id(mandateId)
                .mandateRef("MND-TEST-003")
                .status(MandateStatus.PAUSED)
                .pausedAt(Instant.now())
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(paused));
        when(mandateRepository.save(any(Mandate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mandate result = mandateService.resume(mandateId);

        assertEquals(MandateStatus.ACTIVE, result.getStatus());
        assertNull(result.getPausedAt());
    }

    @Test
    @DisplayName("Cancel mandate records reason")
    void cancelMandateRecordsReason() {
        UUID mandateId = UUID.randomUUID();
        Mandate active = Mandate.builder()
                .id(mandateId)
                .mandateRef("MND-TEST-004")
                .status(MandateStatus.ACTIVE)
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(active));
        when(mandateRepository.save(any(Mandate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mandate result = mandateService.cancel(mandateId, "No longer needed");

        assertEquals(MandateStatus.CANCELLED, result.getStatus());
        assertEquals("No longer needed", result.getCancellationReason());
        assertNotNull(result.getCancelledAt());
    }

    @Test
    @DisplayName("Cannot cancel already cancelled mandate")
    void cannotCancelAlreadyCancelled() {
        UUID mandateId = UUID.randomUUID();
        Mandate cancelled = Mandate.builder()
                .id(mandateId)
                .status(MandateStatus.CANCELLED)
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(cancelled));

        assertThrows(IllegalStateException.class,
                () -> mandateService.cancel(mandateId, "Retry"));
    }

    @Test
    @DisplayName("Revoke one-time mandate during cooling period")
    void revokeOneTimeDuringCooling() {
        UUID mandateId = UUID.randomUUID();
        Mandate oneTime = Mandate.builder()
                .id(mandateId)
                .mandateRef("MND-ONE-001")
                .mandateType("ONE_TIME")
                .status(MandateStatus.ACTIVE)
                .coolingEndsAt(Instant.now().plusSeconds(3600)) // Still in cooling
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(oneTime));
        when(mandateRepository.save(any(Mandate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mandate result = mandateService.revokeDuringCooling(mandateId);

        assertEquals(MandateStatus.CANCELLED, result.getStatus());
        assertEquals("Revoked during cooling period", result.getCancellationReason());
    }

    @Test
    @DisplayName("Cannot revoke after cooling period expired")
    void cannotRevokeAfterCoolingExpired() {
        UUID mandateId = UUID.randomUUID();
        Mandate oneTime = Mandate.builder()
                .id(mandateId)
                .mandateType("ONE_TIME")
                .status(MandateStatus.ACTIVE)
                .coolingEndsAt(Instant.now().minusSeconds(60)) // Cooling ended
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(oneTime));

        assertThrows(IllegalStateException.class,
                () -> mandateService.revokeDuringCooling(mandateId));
    }

    @Test
    @DisplayName("Cannot revoke recurring mandate")
    void cannotRevokeRecurringMandate() {
        UUID mandateId = UUID.randomUUID();
        Mandate recurring = Mandate.builder()
                .id(mandateId)
                .mandateType("RECURRING")
                .status(MandateStatus.ACTIVE)
                .build();

        when(mandateRepository.findById(mandateId)).thenReturn(Optional.of(recurring));

        assertThrows(IllegalStateException.class,
                () -> mandateService.revokeDuringCooling(mandateId));
    }

    @Test
    @DisplayName("Next debit date calculated correctly for each frequency")
    void nextDebitDateCalculation() {
        LocalDate base = LocalDate.of(2025, 6, 1);

        assertEquals(LocalDate.of(2025, 6, 8),
                mandateService.calculateNextDebitDate(base, MandateFrequency.WEEKLY));
        assertEquals(LocalDate.of(2025, 7, 1),
                mandateService.calculateNextDebitDate(base, MandateFrequency.MONTHLY));
        assertEquals(LocalDate.of(2025, 9, 1),
                mandateService.calculateNextDebitDate(base, MandateFrequency.QUARTERLY));
        assertEquals(LocalDate.of(2026, 6, 1),
                mandateService.calculateNextDebitDate(base, MandateFrequency.YEARLY));
        assertNull(mandateService.calculateNextDebitDate(base, MandateFrequency.ONE_TIME));
    }
}
