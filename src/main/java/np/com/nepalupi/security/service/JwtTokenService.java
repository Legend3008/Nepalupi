package np.com.nepalupi.security.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Section 12.6: JWT/OAuth2 Token-Based Authentication Service.
 * <p>
 * Implements a lightweight HMAC-based token system for PSP-to-switch 
 * authentication. Uses HMAC-SHA256 for token signing (no external JWT library needed).
 * <p>
 * Token Lifecycle:
 * <ul>
 *   <li>PSP authenticates with API key → receives access token</li>
 *   <li>Access token valid for 15 minutes</li>
 *   <li>Refresh token valid for 24 hours</li>
 *   <li>Token blacklisting on logout/revocation</li>
 *   <li>Token scope based on PSP tier</li>
 * </ul>
 */
@Service
@Slf4j
public class JwtTokenService {

    private final StringRedisTemplate redisTemplate;
    private final Counter tokensIssued;
    private final Counter tokensRevoked;

    private static final String TOKEN_PREFIX = "token:active:";
    private static final String REFRESH_PREFIX = "token:refresh:";
    private static final String BLACKLIST_PREFIX = "token:blacklisted:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${nepalupi.auth.token.secret:nepal-upi-switch-secret-key-2024}")
    private String tokenSecret;

    @Value("${nepalupi.auth.token.access-ttl-minutes:15}")
    private int accessTokenTtlMinutes;

    @Value("${nepalupi.auth.token.refresh-ttl-hours:24}")
    private int refreshTokenTtlHours;

    public JwtTokenService(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.tokensIssued = meterRegistry.counter("npi.auth.tokens.issued");
        this.tokensRevoked = meterRegistry.counter("npi.auth.tokens.revoked");
    }

    /**
     * Issue an access token for an authenticated PSP.
     *
     * @param pspCode the authenticated PSP
     * @param scopes  permission scopes (PAY, COLLECT, MANDATE, BALANCE, etc.)
     * @return token response with access and refresh tokens
     */
    public TokenResponse issueTokens(String pspCode, Set<String> scopes) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plusSeconds(accessTokenTtlMinutes * 60L);
        Instant refreshExpiry = now.plusSeconds(refreshTokenTtlHours * 3600L);

        // Build access token payload
        String tokenId = UUID.randomUUID().toString();
        String payload = String.join("|", tokenId, pspCode, String.join(",", scopes),
                String.valueOf(accessExpiry.toEpochMilli()));
        String accessToken = payload + "." + computeHmac(payload);

        // Build refresh token
        String refreshId = UUID.randomUUID().toString();
        String refreshPayload = String.join("|", refreshId, pspCode, 
                String.valueOf(refreshExpiry.toEpochMilli()));
        String refreshToken = refreshPayload + "." + computeHmac(refreshPayload);

        // Store in Redis
        Duration accessTtl = Duration.ofMinutes(accessTokenTtlMinutes);
        Duration refreshTtl = Duration.ofHours(refreshTokenTtlHours);

        Map<String, String> tokenMeta = Map.of(
                "pspCode", pspCode,
                "scopes", String.join(",", scopes),
                "issuedAt", now.toString(),
                "expiresAt", accessExpiry.toString()
        );
        tokenMeta.forEach((k, v) -> redisTemplate.opsForHash().put(TOKEN_PREFIX + tokenId, k, v));
        redisTemplate.expire(TOKEN_PREFIX + tokenId, accessTtl);

        redisTemplate.opsForValue().set(REFRESH_PREFIX + refreshId, pspCode, refreshTtl);

        tokensIssued.increment();
        log.info("Tokens issued: psp={}, tokenId={}, expiresAt={}", pspCode, tokenId, accessExpiry);

        return new TokenResponse(accessToken, refreshToken, accessTokenTtlMinutes * 60,
                scopes, "Bearer");
    }

    /**
     * Validate an access token and extract PSP identity.
     *
     * @param token the access token
     * @return validated token info, or empty if invalid
     */
    public Optional<TokenInfo> validateToken(String token) {
        try {
            // Split payload and signature
            int dotIndex = token.lastIndexOf('.');
            if (dotIndex < 0) return Optional.empty();

            String payload = token.substring(0, dotIndex);
            String signature = token.substring(dotIndex + 1);

            // Verify HMAC
            if (!computeHmac(payload).equals(signature)) {
                log.warn("Token signature verification failed");
                return Optional.empty();
            }

            // Parse payload
            String[] parts = payload.split("\\|");
            if (parts.length < 4) return Optional.empty();

            String tokenId = parts[0];
            String pspCode = parts[1];
            Set<String> scopes = Set.of(parts[2].split(","));
            long expiryMillis = Long.parseLong(parts[3]);

            // Check expiry
            if (Instant.now().toEpochMilli() > expiryMillis) {
                log.debug("Token expired: tokenId={}", tokenId);
                return Optional.empty();
            }

            // Check blacklist
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + tokenId))) {
                log.warn("Token is blacklisted: tokenId={}", tokenId);
                return Optional.empty();
            }

            return Optional.of(new TokenInfo(tokenId, pspCode, scopes, Instant.ofEpochMilli(expiryMillis)));
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Refresh an access token using a refresh token.
     */
    public Optional<TokenResponse> refreshAccessToken(String refreshToken) {
        try {
            int dotIndex = refreshToken.lastIndexOf('.');
            String payload = refreshToken.substring(0, dotIndex);
            String signature = refreshToken.substring(dotIndex + 1);

            if (!computeHmac(payload).equals(signature)) {
                return Optional.empty();
            }

            String[] parts = payload.split("\\|");
            String refreshId = parts[0];
            String pspCode = parts[1];
            long expiryMillis = Long.parseLong(parts[2]);

            if (Instant.now().toEpochMilli() > expiryMillis) {
                return Optional.empty();
            }

            // Verify refresh token exists in Redis
            String storedPsp = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshId);
            if (!pspCode.equals(storedPsp)) {
                return Optional.empty();
            }

            // Issue new tokens (with same scopes - default full scope)
            Set<String> defaultScopes = Set.of("PAY", "COLLECT", "BALANCE", "STATUS");
            return Optional.of(issueTokens(pspCode, defaultScopes));
        } catch (Exception e) {
            log.error("Refresh token error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Revoke/blacklist a token.
     */
    public void revokeToken(String tokenId) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + tokenId, "revoked",
                Duration.ofMinutes(accessTokenTtlMinutes + 5));
        redisTemplate.delete(TOKEN_PREFIX + tokenId);
        tokensRevoked.increment();
        log.info("Token revoked: tokenId={}", tokenId);
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    /**
     * Token response.
     */
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            Set<String> scopes,
            String tokenType
    ) {}

    /**
     * Validated token info.
     */
    public record TokenInfo(
            String tokenId,
            String pspCode,
            Set<String> scopes,
            Instant expiresAt
    ) {}
}
