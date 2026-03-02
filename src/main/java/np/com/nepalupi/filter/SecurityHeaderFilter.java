package np.com.nepalupi.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Section 9: OWASP security headers.
 * <p>
 * Adds standard security headers to all responses:
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - X-XSS-Protection: 0 (deprecated, rely on CSP)
 * - Content-Security-Policy: default-src 'self'
 * - Strict-Transport-Security: max-age=31536000
 * - Cache-Control: no-store for API responses
 * - Referrer-Policy: strict-origin-when-cross-origin
 * - Permissions-Policy: restrict sensitive APIs
 */
@Component
@Order(-2) // Runs before rate limiter and auth
@Slf4j
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Prevent MIME type sniffing
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // Modern CSP — no inline scripts, self-origin only
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'");

        // HSTS — enforce HTTPS for 1 year
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Disable XSS filter (rely on CSP instead — modern best practice)
        httpResponse.setHeader("X-XSS-Protection", "0");

        // Referrer policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions policy — restrict access to sensitive browser APIs
        httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(self), payment=(self)");

        // Cache control for API responses
        String uri = ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI();
        if (uri.startsWith("/api/")) {
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
        }

        chain.doFilter(request, response);
    }
}
