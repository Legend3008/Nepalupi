package np.com.nepalupi;

import np.com.nepalupi.filter.RateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Rate Limiting Filter Tests")
class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("Non-API requests bypass rate limiting")
    void nonApiRequestsBypassed() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("First API request passes through")
    void firstRequestPasses() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transactions");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        assertNotNull(response.getHeader("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("IP rate limit exceeded returns 429")
    void ipRateLimitExceeded() throws IOException, ServletException {
        String clientIp = "192.168.1.100";

        // Send 60 requests (the limit)
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/status");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, filterChain);
            assertEquals(200, resp.getStatus(), "Request " + (i + 1) + " should pass");
        }

        // 61st request should be rate limited
        MockHttpServletRequest limitedReq = new MockHttpServletRequest("GET", "/api/v1/status");
        limitedReq.setRemoteAddr(clientIp);
        MockHttpServletResponse limitedResp = new MockHttpServletResponse();
        filter.doFilter(limitedReq, limitedResp, filterChain);

        assertEquals(429, limitedResp.getStatus());
        assertEquals("60", limitedResp.getHeader("Retry-After"));
        assertTrue(limitedResp.getContentAsString().contains("RATE_LIMITED"));
    }

    @Test
    @DisplayName("Different IPs have separate rate limits")
    void differentIpsSeparateLimits() throws IOException, ServletException {
        // Exhaust limit for IP1
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/test");
            req.setRemoteAddr("10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // IP2 should still work
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/v1/test");
        req2.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse resp2 = new MockHttpServletResponse();
        filter.doFilter(req2, resp2, filterChain);

        assertEquals(200, resp2.getStatus());
    }

    @Test
    @DisplayName("PSP rate limit exceeded returns 429")
    void pspRateLimitExceeded() throws IOException, ServletException {
        String pspId = "PSP-NEPAL-001";

        // Send 200 requests (the PSP limit) — use different IPs to avoid IP limit
        for (int i = 0; i < 200; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/transactions");
            req.setRemoteAddr("10.0." + (i / 60) + "." + (i % 60 + 1)); // Rotate IPs
            req.addHeader("X-PSP-ID", pspId);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filter.doFilter(req, resp, filterChain);
        }

        // 201st PSP request should be rate limited
        MockHttpServletRequest limitedReq = new MockHttpServletRequest("POST", "/api/v1/transactions");
        limitedReq.setRemoteAddr("10.1.0.1");
        limitedReq.addHeader("X-PSP-ID", pspId);
        MockHttpServletResponse limitedResp = new MockHttpServletResponse();
        filter.doFilter(limitedReq, limitedResp, filterChain);

        assertEquals(429, limitedResp.getStatus());
        assertTrue(limitedResp.getContentAsString().contains("PSP rate limit"));
    }

    @Test
    @DisplayName("X-Forwarded-For header used for client IP")
    void xForwardedForUsed() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate limit headers present in response")
    void rateLimitHeadersPresent() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setRemoteAddr("172.16.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals("60", response.getHeader("X-RateLimit-Limit"));
        assertNotNull(response.getHeader("X-RateLimit-Remaining"));
    }
}
