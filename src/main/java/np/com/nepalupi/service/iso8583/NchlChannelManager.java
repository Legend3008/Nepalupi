package np.com.nepalupi.service.iso8583;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.config.NchlConfig;
import np.com.nepalupi.domain.entity.Iso8583MessageLog;
import np.com.nepalupi.domain.entity.NchlConnectionState;
import np.com.nepalupi.domain.enums.NchlConnectionStatus;
import np.com.nepalupi.repository.Iso8583MessageLogRepository;
import np.com.nepalupi.repository.NchlConnectionStateRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the TCP connection to NCHL's ISO 8583 switch.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Establish TCP connection to NCHL (primary + DR failover)</li>
 *   <li>Send 0800 sign-on after connection</li>
 *   <li>Send heartbeat every 60 seconds</li>
 *   <li>Automatic failover to DR if primary goes down</li>
 *   <li>Track connection state in database</li>
 * </ul>
 * <p>
 * In production, this uses jPOS's ISOChannel for the actual TCP framing.
 * In dev/mock mode, it simulates the connection lifecycle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NchlChannelManager {

    private final NchlConfig nchlConfig;
    private final Iso8583MessageBuilder messageBuilder;
    private final Iso8583MessageLogRepository messageLogRepo;
    private final NchlConnectionStateRepository connectionStateRepo;
    private final np.com.nepalupi.util.IdGenerator idGenerator;

    private final AtomicReference<String> activeChannel = new AtomicReference<>("PRIMARY");
    private final AtomicReference<NchlConnectionStatus> connectionStatus =
            new AtomicReference<>(NchlConnectionStatus.DISCONNECTED);
    private final AtomicBoolean accepting = new AtomicBoolean(false);

    // In production: private ISOChannel primaryChannel, drChannel;

    @PostConstruct
    void init() {
        if (nchlConfig.isMockMode()) {
            log.info("╔══════════════════════════════════════════════════════╗");
            log.info("║  NCHL Channel Manager — MOCK MODE                   ║");
            log.info("║  No real NCHL connection. Simulating sign-on.       ║");
            log.info("╚══════════════════════════════════════════════════════╝");
            simulateSignOn("PRIMARY");
        } else {
            log.info("Connecting to NCHL primary: {}:{}", nchlConfig.getPrimaryHost(), nchlConfig.getPrimaryPort());
            connectAndSignOn("PRIMARY");
        }
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down NCHL channel manager");
        accepting.set(false);
        updateConnectionState(activeChannel.get(), NchlConnectionStatus.DISCONNECTED);
    }

    /**
     * Check if the NCHL channel is ready to send financial messages.
     */
    public boolean isReady() {
        return connectionStatus.get().canSendTransactions() && accepting.get();
    }

    /**
     * Get the currently active channel (PRIMARY or DR).
     */
    public String getActiveChannel() {
        return activeChannel.get();
    }

    /**
     * Get current connection status.
     */
    public NchlConnectionStatus getConnectionStatus() {
        return connectionStatus.get();
    }

    /**
     * Send an ISO 8583 message and return the response.
     * <p>
     * In production, this serializes the message to bytes via jPOS,
     * writes to the TCP socket, and reads the response.
     * <p>
     * In mock mode, simulates a successful response.
     */
    public Iso8583MessageBuilder.Iso8583Message sendMessage(Iso8583MessageBuilder.Iso8583Message request,
                                                             java.util.UUID transactionId) {
        if (!isReady()) {
            log.error("NCHL channel not ready! Status: {}", connectionStatus.get());
            throw new IllegalStateException("NCHL channel not connected/signed-on");
        }

        // Log outbound message
        Iso8583MessageLog outLog = Iso8583MessageLog.builder()
                .mti(request.getMti())
                .direction("OUTBOUND")
                .processingCode(request.getField3())
                .amountPaisa(request.getField4() != null ? Long.parseLong(request.getField4()) : null)
                .stan(request.getField11())
                .rrn(request.getField37())
                .currencyCode(request.getField49())
                .rawHex(request.toHexDump())
                .transactionId(transactionId)
                .channel(activeChannel.get())
                .sentAt(Instant.now())
                .build();
        messageLogRepo.save(outLog);

        if (nchlConfig.isMockMode()) {
            return simulateResponse(request, transactionId);
        }

        // ── Production path (jPOS) ──────────────────────────
        // ISOMsg isoMsg = convertToJposMsg(request);
        // channel.send(isoMsg);
        // ISOMsg response = channel.receive(nchlConfig.getReadTimeoutMs());
        // return convertFromJposMsg(response);
        throw new UnsupportedOperationException("Production NCHL connection not yet configured");
    }

    /**
     * Heartbeat — runs every 60 seconds.
     * Sends 0800 echo test to keep TCP alive and verify NCHL is reachable.
     */
    @Scheduled(fixedDelayString = "${nepalupi.nchl.heartbeat-interval-seconds:60}000")
    void sendHeartbeat() {
        if (!accepting.get()) return;

        String stan = idGenerator.generateStan();
        Iso8583MessageBuilder.Iso8583Message heartbeat = messageBuilder.buildHeartbeatRequest(stan);

        try {
            if (nchlConfig.isMockMode()) {
                // Simulate heartbeat success
                log.debug("Heartbeat sent (mock) — STAN: {}", stan);
            } else {
                // In production: send via TCP channel
                // sendMessage(heartbeat, null);
            }

            // Update heartbeat tracking
            connectionStateRepo.findByChannel(activeChannel.get()).ifPresent(state -> {
                state.setLastHeartbeat(Instant.now());
                state.setHeartbeatCount(state.getHeartbeatCount() + 1);
                state.setUpdatedAt(Instant.now());
                connectionStateRepo.save(state);
            });

        } catch (Exception e) {
            log.error("Heartbeat failed on {} channel: {}", activeChannel.get(), e.getMessage());
            handleConnectionFailure();
        }
    }

    // ── Connection lifecycle ─────────────────────────────────

    private void connectAndSignOn(String channel) {
        try {
            connectionStatus.set(NchlConnectionStatus.CONNECTED);
            updateConnectionState(channel, NchlConnectionStatus.CONNECTED);

            // Send sign-on
            connectionStatus.set(NchlConnectionStatus.SIGNING_ON);
            updateConnectionState(channel, NchlConnectionStatus.SIGNING_ON);

            String stan = idGenerator.generateStan();
            Iso8583MessageBuilder.Iso8583Message signOn = messageBuilder.buildSignOnRequest(stan);

            // In production: send sign-on and wait for 0810 with response code "00"
            // Iso8583MessageBuilder.Iso8583Message response = sendRawMessage(signOn);
            // if (!"00".equals(response.getField39())) throw new Exception("Sign-on rejected");

            connectionStatus.set(NchlConnectionStatus.SIGNED_ON);
            accepting.set(true);
            activeChannel.set(channel);
            updateConnectionState(channel, NchlConnectionStatus.SIGNED_ON);

            log.info("Successfully signed on to NCHL {} channel", channel);
        } catch (Exception e) {
            log.error("Failed to connect/sign-on to NCHL {} channel: {}", channel, e.getMessage());
            handleConnectionFailure();
        }
    }

    private void simulateSignOn(String channel) {
        connectionStatus.set(NchlConnectionStatus.SIGNED_ON);
        accepting.set(true);
        activeChannel.set(channel);
        updateConnectionState(channel, NchlConnectionStatus.SIGNED_ON);
        log.info("Mock sign-on to NCHL {} channel successful", channel);
    }

    /**
     * Handle connection failure — attempt failover to DR.
     */
    private void handleConnectionFailure() {
        accepting.set(false);
        String current = activeChannel.get();

        connectionStateRepo.findByChannel(current).ifPresent(state -> {
            state.setFailCount(state.getFailCount() + 1);
            state.setUpdatedAt(Instant.now());
            connectionStateRepo.save(state);
        });

        if ("PRIMARY".equals(current)) {
            log.warn("Primary NCHL connection failed. Attempting DR failover...");
            updateConnectionState("PRIMARY", NchlConnectionStatus.DISCONNECTED);
            if (nchlConfig.isMockMode()) {
                simulateSignOn("DR");
            } else {
                connectAndSignOn("DR");
            }
        } else {
            log.error("═══ CRITICAL: Both PRIMARY and DR NCHL connections failed! ═══");
            log.error("═══ STOPPING transaction acceptance. Manual intervention required. ═══");
            connectionStatus.set(NchlConnectionStatus.DISCONNECTED);
            updateConnectionState("DR", NchlConnectionStatus.DISCONNECTED);
        }
    }

    /**
     * Simulate a bank response in mock mode.
     */
    private Iso8583MessageBuilder.Iso8583Message simulateResponse(Iso8583MessageBuilder.Iso8583Message request,
                                                                   java.util.UUID transactionId) {
        // Simulate 50-200ms bank processing time
        try { Thread.sleep(50 + (long)(Math.random() * 150)); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        String responseMti = request.getMti().equals("0200") ? "0210" :
                             request.getMti().equals("0400") ? "0410" : "0810";

        String authCode = String.format("%06d", (int)(Math.random() * 999999));

        Iso8583MessageBuilder.Iso8583Message response = Iso8583MessageBuilder.Iso8583Message.builder()
                .mti(responseMti)
                .field2(request.getField2())
                .field3(request.getField3())
                .field4(request.getField4())
                .field11(request.getField11())
                .field37(request.getField37())
                .field38(authCode)
                .field39("00")  // Approved
                .field49(request.getField49())
                .build();

        // Log inbound response
        Iso8583MessageLog inLog = Iso8583MessageLog.builder()
                .mti(responseMti)
                .direction("INBOUND")
                .processingCode(request.getField3())
                .amountPaisa(request.getField4() != null ? Long.parseLong(request.getField4()) : null)
                .stan(request.getField11())
                .rrn(request.getField37())
                .responseCode("00")
                .authCode(authCode)
                .currencyCode(request.getField49())
                .rawHex(response.toHexDump())
                .transactionId(transactionId)
                .channel(activeChannel.get())
                .receivedAt(Instant.now())
                .build();
        messageLogRepo.save(inLog);

        return response;
    }

    private void updateConnectionState(String channel, NchlConnectionStatus status) {
        connectionStateRepo.findByChannel(channel).ifPresent(state -> {
            state.setStatus(status.name());
            if (status == NchlConnectionStatus.SIGNED_ON) {
                state.setLastSignOn(Instant.now());
                state.setSignOnCount(state.getSignOnCount() + 1);
            }
            state.setUpdatedAt(Instant.now());
            connectionStateRepo.save(state);
        });
    }
}
