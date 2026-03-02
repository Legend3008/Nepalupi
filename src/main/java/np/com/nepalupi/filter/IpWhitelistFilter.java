package np.com.nepalupi.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.IpWhitelist;
import np.com.nepalupi.repository.IpWhitelistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Section 9.4: IP whitelisting for PSP connections.
 * <p>
 * When enabled, only allows requests from registered IP addresses.
 * IP whitelist is loaded from database and refreshed periodically.
 * Disabled by default in dev mode.
 */
@Component
@Order(-1) // Runs after security headers, before rate limiter
@RequiredArgsConstructor
@Slf4j
public class IpWhitelistFilter implements Filter {

    private final IpWhitelistRepository ipWhitelistRepository;

    @Value("${nepalupi.security.ip-whitelist-enabled:false}")
    private boolean ipWhitelistEnabled;

    private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
    private volatile long lastRefreshTime = 0;
    private static final long REFRESH_INTERVAL_MS = 300_000; // 5 minutes

    @PostConstruct
    void init() {
        if (ipWhitelistEnabled) {
            refreshWhitelist();
            log.info("IP whitelist filter ENABLED — {} IPs loaded", allowedIps.size());
        } else {
            log.info("IP whitelist filter DISABLED (dev mode)");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!ipWhitelistEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String uri = httpRequest.getRequestURI();

        // Only enforce on API endpoints
        if (!uri.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // Allow health checks
        if (uri.startsWith("/actuator/") || uri.equals("/health")) {
            chain.doFilter(request, response);
            return;
        }

        // Refresh whitelist periodically
        if (System.currentTimeMillis() - lastRefreshTime > REFRESH_INTERVAL_MS) {
            refreshWhitelist();
        }

        String clientIp = extractClientIp(httpRequest);

        if (!allowedIps.contains(clientIp) && !isLocalhost(clientIp)) {
            log.warn("IP whitelist DENIED: ip={}, uri={}", clientIp, uri);
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    "{\"error\":\"IP_NOT_WHITELISTED\",\"message\":\"Your IP is not authorized\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private void refreshWhitelist() {
        try {
            List<IpWhitelist> entries = ipWhitelistRepository.findByIsActiveTrue();
            allowedIps.clear();
            for (IpWhitelist entry : entries) {
                if (entry.getExpiresAt() == null || entry.getExpiresAt().isAfter(Instant.now())) {
                    allowedIps.add(entry.getIpAddress());
                }
            }
            lastRefreshTime = System.currentTimeMillis();
            log.debug("IP whitelist refreshed: {} active IPs", allowedIps.size());
        } catch (Exception e) {
            log.error("Failed to refresh IP whitelist: {}", e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }
}
