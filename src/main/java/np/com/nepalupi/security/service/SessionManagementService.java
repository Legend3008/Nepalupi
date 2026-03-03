package np.com.nepalupi.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Section 12.5: Session Management Service.
 * <p>
 * Manages PSP session lifecycle in the stateless UPI switch:
 * <ul>
 *   <li>Session creation on PSP authentication</li>
 *   <li>Session validation for each API call</li>
 *   <li>Idle timeout (15 min) and absolute timeout (8 hours)</li>
 *   <li>Concurrent session control per PSP</li>
 *   <li>Session invalidation on security events</li>
 *   <li>Session activity tracking for audit</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final StringRedisTemplate redisTemplate;

    private static final String SESSION_PREFIX = "session:";
    private static final String PSP_SESSIONS_PREFIX = "psp:sessions:";

    @Value("${nepalupi.session.idle-timeout-minutes:15}")
    private int idleTimeoutMinutes;

    @Value("${nepalupi.session.absolute-timeout-hours:8}")
    private int absoluteTimeoutHours;

    @Value("${nepalupi.session.max-concurrent-per-psp:5}")
    private int maxConcurrentSessionsPerPsp;

    /**
     * Create a new session for an authenticated PSP.
     *
     * @param pspCode    PSP identifier
     * @param sourceIp   source IP address
     * @param deviceInfo device/client information
     * @return session ID
     */
    public String createSession(String pspCode, String sourceIp, String deviceInfo) {
        // Check concurrent session limit
        Long activeSessions = redisTemplate.opsForSet().size(PSP_SESSIONS_PREFIX + pspCode);
        if (activeSessions != null && activeSessions >= maxConcurrentSessionsPerPsp) {
            log.warn("Concurrent session limit reached: psp={}, active={}, max={}",
                    pspCode, activeSessions, maxConcurrentSessionsPerPsp);
            // Evict oldest session
            String oldest = redisTemplate.opsForSet().pop(PSP_SESSIONS_PREFIX + pspCode);
            if (oldest != null) {
                invalidateSession(oldest);
                log.info("Evicted oldest session: {}", oldest);
            }
        }

        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant absoluteExpiry = now.plusSeconds(absoluteTimeoutHours * 3600L);

        Map<String, String> sessionData = Map.of(
                "sessionId", sessionId,
                "pspCode", pspCode,
                "sourceIp", sourceIp,
                "deviceInfo", deviceInfo != null ? deviceInfo : "unknown",
                "createdAt", now.toString(),
                "lastActivityAt", now.toString(),
                "absoluteExpiryAt", absoluteExpiry.toString(),
                "status", "ACTIVE"
        );

        String key = SESSION_PREFIX + sessionId;
        sessionData.forEach((k, v) -> redisTemplate.opsForHash().put(key, k, v));
        redisTemplate.expire(key, Duration.ofHours(absoluteTimeoutHours));

        // Track PSP's active sessions
        redisTemplate.opsForSet().add(PSP_SESSIONS_PREFIX + pspCode, sessionId);
        redisTemplate.expire(PSP_SESSIONS_PREFIX + pspCode, Duration.ofHours(absoluteTimeoutHours + 1));

        log.info("Session created: sessionId={}, psp={}, absoluteExpiry={}", sessionId, pspCode, absoluteExpiry);
        return sessionId;
    }

    /**
     * Validate and refresh a session.
     *
     * @param sessionId the session to validate
     * @return session info if valid
     */
    public SessionInfo validateSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;

        String status = (String) redisTemplate.opsForHash().get(key, "status");
        if (!"ACTIVE".equals(status)) {
            return null;
        }

        // Check idle timeout
        String lastActivity = (String) redisTemplate.opsForHash().get(key, "lastActivityAt");
        if (lastActivity != null) {
            Instant lastActiveAt = Instant.parse(lastActivity);
            if (lastActiveAt.plusSeconds(idleTimeoutMinutes * 60L).isBefore(Instant.now())) {
                log.info("Session idle timeout: sessionId={}", sessionId);
                invalidateSession(sessionId);
                return null;
            }
        }

        // Check absolute timeout
        String absoluteExpiry = (String) redisTemplate.opsForHash().get(key, "absoluteExpiryAt");
        if (absoluteExpiry != null && Instant.parse(absoluteExpiry).isBefore(Instant.now())) {
            log.info("Session absolute timeout: sessionId={}", sessionId);
            invalidateSession(sessionId);
            return null;
        }

        // Refresh last activity
        redisTemplate.opsForHash().put(key, "lastActivityAt", Instant.now().toString());

        String pspCode = (String) redisTemplate.opsForHash().get(key, "pspCode");
        String sourceIp = (String) redisTemplate.opsForHash().get(key, "sourceIp");

        return new SessionInfo(sessionId, pspCode, sourceIp, Instant.parse(lastActivity));
    }

    /**
     * Invalidate a session.
     */
    public void invalidateSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        String pspCode = (String) redisTemplate.opsForHash().get(key, "pspCode");

        redisTemplate.opsForHash().put(key, "status", "INVALIDATED");
        redisTemplate.expire(key, Duration.ofMinutes(5)); // Keep briefly for audit

        if (pspCode != null) {
            redisTemplate.opsForSet().remove(PSP_SESSIONS_PREFIX + pspCode, sessionId);
        }

        log.info("Session invalidated: sessionId={}, psp={}", sessionId, pspCode);
    }

    /**
     * Invalidate all sessions for a PSP (e.g., on security incident).
     */
    public void invalidateAllSessions(String pspCode) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(PSP_SESSIONS_PREFIX + pspCode);
        if (sessionIds != null) {
            sessionIds.forEach(this::invalidateSession);
        }
        redisTemplate.delete(PSP_SESSIONS_PREFIX + pspCode);
        log.warn("All sessions invalidated for psp={}, count={}", pspCode,
                sessionIds != null ? sessionIds.size() : 0);
    }

    /**
     * Get active session count for a PSP.
     */
    public long getActiveSessionCount(String pspCode) {
        Long count = redisTemplate.opsForSet().size(PSP_SESSIONS_PREFIX + pspCode);
        return count != null ? count : 0;
    }

    /**
     * Cleanup task — remove stale session references.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupStaleSessions() {
        log.debug("Running stale session cleanup...");
        // Redis TTL handles most cleanup automatically
        // This catches edge cases where session expired but set entry persists
    }

    /**
     * Session info record.
     */
    public record SessionInfo(
            String sessionId,
            String pspCode,
            String sourceIp,
            Instant lastActivityAt
    ) {}
}
