package np.com.nepalupi.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Token-bucket rate limiter per IP / PSP-ID.
 * <p>
 * Limits:
 * - Per IP:  60 requests/minute (burst 10)
 * - Per PSP: 200 requests/minute (burst 20)
 * <p>
 * Uses a sliding window counter with atomic operations — no external dependency.
 * In production with multiple instances, replace with Redis-based rate limiter.
 */
@Component
@Order(0)  // Runs before PspAuthFilter
@Slf4j
public class RateLimitingFilter implements Filter {

    private static final int IP_LIMIT_PER_MINUTE = 60;
    private static final int PSP_LIMIT_PER_MINUTE = 200;
    private static final long WINDOW_MS = 60_000L;

    private final Map<String, SlidingWindow> ipWindows = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindow> pspWindows = new ConcurrentHashMap<>();

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
        SlidingWindow ipWindow = ipWindows.computeIfAbsent(clientIp, k -> new SlidingWindow());
        if (!ipWindow.tryAcquire(IP_LIMIT_PER_MINUTE)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            sendRateLimitError(response, "IP rate limit exceeded. Max " + IP_LIMIT_PER_MINUTE + " req/min.");
            return;
        }

        // Rate limit by PSP
        String pspId = request.getHeader("X-PSP-ID");
        if (pspId != null) {
            SlidingWindow pspWindow = pspWindows.computeIfAbsent(pspId, k -> new SlidingWindow());
            if (!pspWindow.tryAcquire(PSP_LIMIT_PER_MINUTE)) {
                log.warn("Rate limit exceeded for PSP: {}", pspId);
                sendRateLimitError(response, "PSP rate limit exceeded. Max " + PSP_LIMIT_PER_MINUTE + " req/min.");
                return;
            }
        }

        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(IP_LIMIT_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, IP_LIMIT_PER_MINUTE - ipWindow.getCount())));

        chain.doFilter(request, response);
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
     * Simple sliding window counter using fixed-window approximation.
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
