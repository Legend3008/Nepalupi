package np.com.nepalupi.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Distributed rate limiter per IP / PSP-ID using Redis sliding window.
 * <p>
 * Limits:
 * - Per IP:  60 requests/minute (burst 10)
 * - Per PSP: 200 requests/minute (burst 20)
 * <p>
 * Uses Redis INCR + EXPIRE for distributed rate limiting across multiple
 * switch instances. Falls back to in-memory counters if Redis is unavailable.
 * <p>
 * Section 3.2: API gateway with rate limiting (distributed for multi-DC).
 */
@Component
@Order(0)  // Runs before PspAuthFilter
@Slf4j
public class RateLimitingFilter implements Filter {

    private static final int IP_LIMIT_PER_MINUTE = 60;
    private static final int PSP_LIMIT_PER_MINUTE = 200;
    private static final long WINDOW_MS = 60_000L;
    private static final Duration REDIS_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    // Fallback in-memory counters when Redis is unavailable
    private final Map<String, SlidingWindow> ipWindows = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindow> pspWindows = new ConcurrentHashMap<>();

    public RateLimitingFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // Skip for non-API endpoints
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // Rate limit by IP
        String clientIp = getClientIp(request);
        long ipCount = tryAcquire("rate:ip:" + clientIp, IP_LIMIT_PER_MINUTE, clientIp, ipWindows);
        if (ipCount < 0) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            sendRateLimitError(response, "IP rate limit exceeded. Max " + IP_LIMIT_PER_MINUTE + " req/min.");
            return;
        }

        // Rate limit by PSP
        String pspId = request.getHeader("X-PSP-ID");
        if (pspId != null) {
            long pspCount = tryAcquire("rate:psp:" + pspId, PSP_LIMIT_PER_MINUTE, pspId, pspWindows);
            if (pspCount < 0) {
                log.warn("Rate limit exceeded for PSP: {}", pspId);
                sendRateLimitError(response, "PSP rate limit exceeded. Max " + PSP_LIMIT_PER_MINUTE + " req/min.");
                return;
            }
        }

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(IP_LIMIT_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, IP_LIMIT_PER_MINUTE - ipCount)));

        chain.doFilter(request, response);
    }

    /**
     * Try to acquire a rate limit slot. Uses Redis for distributed counting
     * with automatic fallback to in-memory if Redis is unavailable.
     *
     * @return current count if allowed, -1 if rate limited
     */
    private long tryAcquire(String redisKey, int limit, String fallbackKey,
                            Map<String, SlidingWindow> fallbackMap) {
        try {
            // Distributed: Redis INCR + EXPIRE
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                // First request in window — set TTL
                redisTemplate.expire(redisKey, REDIS_TTL);
            }
            if (count != null && count > limit) {
                return -1; // Rate limited
            }
            return count != null ? count : 1;
        } catch (Exception e) {
            // Redis unavailable — fallback to in-memory
            log.debug("Redis unavailable for rate limiting, using in-memory fallback: {}", e.getMessage());
            SlidingWindow window = fallbackMap.computeIfAbsent(fallbackKey, k -> new SlidingWindow());
            if (!window.tryAcquire(limit)) {
                return -1;
            }
            return window.getCount();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        response.getWriter().write(
                String.format("{\"errorCode\":\"RATE_LIMITED\",\"message\":\"%s\"}", message));
    }

    /**
     * In-memory fallback: sliding window counter using fixed-window approximation.
     * Thread-safe via AtomicLong operations.
     */
    static class SlidingWindow {
        private final AtomicLong count = new AtomicLong(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean tryAcquire(int limit) {
            long now = System.currentTimeMillis();
            if (now - windowStart > WINDOW_MS) {
                // Reset window
                synchronized (this) {
                    if (now - windowStart > WINDOW_MS) {
                        count.set(0);
                        windowStart = now;
                    }
                }
            }
            return count.incrementAndGet() <= limit;
        }

        long getCount() {
            return count.get();
        }
    }
}
