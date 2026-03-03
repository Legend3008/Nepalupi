package np.com.nepalupi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Section 15.2 / 20.6: API Versioning Strategy.
 * <p>
 * Supports three versioning mechanisms (in priority order):
 * <ol>
 *   <li>URL path versioning: /api/v1/pay, /api/v2/pay</li>
 *   <li>Header versioning: X-API-Version: 2</li>
 *   <li>Accept header: Accept: application/vnd.npi.v2+json</li>
 * </ol>
 * Current supported versions: v1 (stable), v2 (beta).
 * Deprecated versions return Sunset + Deprecation headers per RFC 8594.
 */
@Component
@Slf4j
public class ApiVersionInterceptor implements HandlerInterceptor {

    private static final String VERSION_HEADER = "X-API-Version";
    private static final String ACCEPT_VERSION_PATTERN = "application/vnd\\.npi\\.v(\\d+)\\+json";
    private static final String RESPONSE_VERSION_HEADER = "X-API-Version";
    private static final String DEPRECATION_HEADER = "Deprecation";
    private static final String SUNSET_HEADER = "Sunset";

    @Value("${nepalupi.api.current-version:1}")
    private int currentVersion;

    @Value("${nepalupi.api.min-version:1}")
    private int minVersion;

    @Value("${nepalupi.api.max-version:2}")
    private int maxVersion;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        int requestedVersion = resolveVersion(request);

        // Validate version range
        if (requestedVersion < minVersion || requestedVersion > maxVersion) {
            log.warn("Unsupported API version requested: {}, path={}", requestedVersion, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader("X-Error", "Unsupported API version: " + requestedVersion +
                    ". Supported: v" + minVersion + "-v" + maxVersion);
            return false;
        }

        // Set version in request attribute for controllers
        request.setAttribute("apiVersion", requestedVersion);

        // Add response headers
        response.setHeader(RESPONSE_VERSION_HEADER, String.valueOf(requestedVersion));

        // Mark deprecated versions
        if (requestedVersion < currentVersion) {
            response.setHeader(DEPRECATION_HEADER, "true");
            response.setHeader(SUNSET_HEADER, "2025-12-31T00:00:00Z");
            response.setHeader("Link", "</api/v" + currentVersion + ">; rel=\"successor-version\"");
            log.debug("Deprecated API version {} used: path={}", requestedVersion, request.getRequestURI());
        }

        return true;
    }

    /**
     * Resolve the API version from the request.
     * Priority: URL path > X-API-Version header > Accept header > default.
     */
    private int resolveVersion(HttpServletRequest request) {
        // 1. URL path: /api/v2/pay
        String path = request.getRequestURI();
        java.util.regex.Matcher pathMatcher = java.util.regex.Pattern
                .compile("/api/v(\\d+)/").matcher(path);
        if (pathMatcher.find()) {
            return Integer.parseInt(pathMatcher.group(1));
        }

        // 2. X-API-Version header
        String versionHeader = request.getHeader(VERSION_HEADER);
        if (versionHeader != null) {
            try {
                return Integer.parseInt(versionHeader.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid X-API-Version header: {}", versionHeader);
            }
        }

        // 3. Accept header: application/vnd.npi.v2+json
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null) {
            java.util.regex.Matcher acceptMatcher = java.util.regex.Pattern
                    .compile(ACCEPT_VERSION_PATTERN).matcher(acceptHeader);
            if (acceptMatcher.find()) {
                return Integer.parseInt(acceptMatcher.group(1));
            }
        }

        // Default to current version
        return currentVersion;
    }
}
