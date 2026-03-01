package np.com.nepalupi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * NCHL connection configuration.
 * <p>
 * In production, the host/port comes from NCHL after your leased line is provisioned.
 * Credentials are loaded from HSM/Vault, not application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "nepalupi.nchl")
@Getter @Setter
public class NchlConfig {

    /** Primary NCHL switch IP (leased line) */
    private String primaryHost = "127.0.0.1";
    private int primaryPort = 5500;

    /** DR NCHL switch IP (disaster recovery) */
    private String drHost = "127.0.0.1";
    private int drPort = 5501;

    /** Connection timeout in ms */
    private int connectionTimeoutMs = 10000;

    /** Read timeout for response in ms (30s as per spec) */
    private int readTimeoutMs = 30000;

    /** Heartbeat interval in seconds */
    private int heartbeatIntervalSeconds = 60;

    /** Terminal ID assigned by NCHL */
    private String terminalId = "NUPI0001";

    /** Merchant/Acquirer ID assigned by NCHL */
    private String acquirerId = "NEPALUPI";

    /** Whether to use mock mode (no real NCHL connection) */
    private boolean mockMode = true;
}
