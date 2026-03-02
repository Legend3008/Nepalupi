package np.com.nepalupi.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Mutual TLS (mTLS) configuration for secure PSP-to-switch communication.
 * <p>
 * Section 3.2: mTLS connection to NPCI
 * Section 3.4: Secure communication between switch and bank CBS
 * <p>
 * In the UPI architecture:
 * - Each PSP connects to the switch using mutual TLS
 * - PSP presents its client certificate (registered during onboarding)
 * - Switch validates the certificate against the trust store
 * - Switch presents its server certificate to the PSP
 * <p>
 * Configuration (activate with --spring.profiles.active=production):
 * <pre>
 * server:
 *   ssl:
 *     enabled: true
 *     key-store: classpath:keystore.p12          # Switch's private key + cert
 *     key-store-password: ${KEYSTORE_PASSWORD}
 *     key-store-type: PKCS12
 *     trust-store: classpath:truststore.p12      # PSP client certs
 *     trust-store-password: ${TRUSTSTORE_PASSWORD}
 *     client-auth: need                          # Enforce mTLS
 * </pre>
 * <p>
 * Certificate hierarchy:
 * <pre>
 * NRB Root CA
 * ├── NCHL Intermediate CA
 * │   ├── Switch Server Cert (TLS_SERVER)
 * │   ├── Switch Client Cert (TLS_CLIENT — for connecting to NCHL)
 * │   └── MTLS Service Cert (MTLS_SERVICE — inter-service)
 * └── PSP Intermediate CA
 *     ├── PSP-NABIL Client Cert
 *     ├── PSP-GLOBAL Client Cert
 *     └── PSP-SBI Client Cert
 * </pre>
 * <p>
 * PRODUCTION NOTE: In development, SSL is disabled. Enable via production profile.
 * Key store and trust store files must be provisioned separately (not in repo).
 */
@Configuration
@Slf4j
public class MtlsConfig {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.ssl.client-auth:none}")
    private String clientAuth;

    @Value("${nepalupi.security.hmac-enabled:false}")
    private boolean hmacEnabled;

    @PostConstruct
    public void logSecurityStatus() {
        log.info("══════════════════════════════════════════════");
        log.info("  Security Configuration Status");
        log.info("  SSL/TLS enabled: {}", sslEnabled);
        log.info("  mTLS client auth: {}", clientAuth);
        log.info("  HMAC request signing: {}", hmacEnabled);

        if (!sslEnabled) {
            log.warn("  ⚠ SSL/TLS DISABLED — not suitable for production!");
            log.warn("  Enable with: server.ssl.enabled=true");
            log.warn("  Or activate production profile: --spring.profiles.active=production");
        }

        if (!"need".equals(clientAuth) && !"want".equals(clientAuth)) {
            log.warn("  ⚠ mTLS not enforced — PSP client certificates not validated at transport layer");
        }

        if (!hmacEnabled) {
            log.warn("  ⚠ HMAC request signing DISABLED — requests not cryptographically verified");
        }

        if (sslEnabled && "need".equals(clientAuth) && hmacEnabled) {
            log.info("  ✓ Full security enabled: SSL + mTLS + HMAC");
        }
        log.info("══════════════════════════════════════════════");
    }
}
