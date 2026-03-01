package np.com.nepalupi.operations.enums;

/**
 * Incident severity levels — aligned with Google SRE model.
 * <p>
 * SEV1: Full system down / data breach — CEO/NRB notified
 * SEV2: Major degradation — on-call + engineering lead
 * SEV3: Partial degradation — on-call engineer
 * SEV4: Minor issue — logged, fixed during business hours
 */
public enum IncidentSeverity {
    SEV1(1, 5,   "Full outage or data breach. All hands."),
    SEV2(2, 15,  "Major service degradation. On-call + lead."),
    SEV3(3, 60,  "Partial degradation. On-call engineer."),
    SEV4(4, 480, "Minor issue. Next business day.");

    private final int level;
    private final int acknowledgeSlaMinutes;
    private final String description;

    IncidentSeverity(int level, int acknowledgeSlaMinutes, String description) {
        this.level = level;
        this.acknowledgeSlaMinutes = acknowledgeSlaMinutes;
        this.description = description;
    }

    public int getLevel() { return level; }
    public int getAcknowledgeSlaMinutes() { return acknowledgeSlaMinutes; }
    public String getDescription() { return description; }
}
