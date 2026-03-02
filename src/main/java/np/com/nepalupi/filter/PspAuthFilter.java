package np.com.nepalupi.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import np.com.nepalupi.domain.entity.Psp;
import np.com.nepalupi.repository.PspRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Authenticates every PSP API request using HMAC-SHA256 signature verification.
 * <p>
 * Each request must include:
 * <ul>
 *   <li>X-PSP-ID: the registered PSP identifier</li>
 *   <li>X-Timestamp: ISO-8601 timestamp</li>
 *   <li>X-Signature: HMAC-SHA256(pspId + timestamp + body, pspSecret)</li>
 * </ul>
 * <p>
 * Replay attack prevention: requests older than 5 minutes are rejected.
 * Dev mode bypass: when nepalupi.security.hmac-enabled=false, skips signature check.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PspAuthFilter implements Filter {

    private static final long MAX_REQUEST_AGE_SECONDS = 300; // 5 minutes
    private final PspRepository pspRepository;

    @Value("${nepalupi.security.hmac-enabled:false}")
    private boolean hmacEnabled;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        // Skip auth for health checks, actuator, and swagger endpoints
        if (path.startsWith("/actuator") || path.equals("/health")
                || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        // Only apply to /api/ endpoints
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String pspId = request.getHeader("X-PSP-ID");
        String timestamp = request.getHeader("X-Timestamp");
        String signature = request.getHeader("X-Signature");

        // Dev mode: allow requests without auth headers
        if (pspId == null && timestamp == null && signature == null) {
            log.debug("No PSP auth headers — allowing in dev mode");
            chain.doFilter(request, response);
            return;
        }

        if (pspId == null || timestamp == null || signature == null) {
            sendError(response, 401, "Missing authentication headers");
            return;
        }

        // Reject expired requests (replay attack prevention)
        try {
            Instant requestTime = Instant.parse(timestamp);
            long ageSeconds = Instant.now().getEpochSecond() - requestTime.getEpochSecond();
            if (ageSeconds > MAX_REQUEST_AGE_SECONDS || ageSeconds < -30) {
                sendError(response, 401, "Request expired or clock skew detected");
                return;
            }
        } catch (Exception e) {
            sendError(response, 401, "Invalid timestamp format");
            return;
        }

        // Verify PSP exists and is active
        Psp psp = pspRepository.findByPspIdAndIsActiveTrue(pspId).orElse(null);
        if (psp == null) {
            sendError(response, 401, "Unknown or inactive PSP");
            return;
        }

        // HMAC-SHA256 signature verification
        if (hmacEnabled) {
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            String body = cachedRequest.getCachedBody();
            String dataToSign = pspId + timestamp + body;
            String expectedSig = computeHmac(dataToSign, psp.getSecretHash());

            if (!signature.equals(expectedSig)) {
                log.warn("HMAC signature mismatch for PSP {}", pspId);
                sendError(response, 401, "Invalid signature");
                return;
            }

            log.debug("HMAC verified for PSP {}", pspId);
            chain.doFilter(cachedRequest, response);
        } else {
            log.debug("HMAC verification skipped (disabled) for PSP {}", pspId);
            chain.doFilter(request, response);
        }
    }

    private String computeHmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"errorCode\":\"AUTH_FAILED\",\"message\":\"%s\"}", message));
    }
}
