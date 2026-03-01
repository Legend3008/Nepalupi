package np.com.nepalupi.security.enums;

public enum WafRuleType {
    SQL_INJECTION,
    XSS,
    REQUEST_SMUGGLING,
    RATE_LIMIT,
    GEO_BLOCK,
    BOT_DETECTION,
    CUSTOM
}
