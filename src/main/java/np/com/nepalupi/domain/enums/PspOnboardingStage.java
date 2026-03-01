package np.com.nepalupi.domain.enums;

/**
 * PSP Onboarding stages — 6 stages from application to full production.
 */
public enum PspOnboardingStage {

    /** Initial application submitted */
    APPLICATION,

    /** Legal PSP Participation Agreement being signed */
    LEGAL_AGREEMENT,

    /** Sandbox access granted, running certification test suite */
    TECHNICAL_CERTIFICATION,

    /** Security team reviewing integration */
    SECURITY_REVIEW,

    /** Live in production with conservative limits */
    PILOT,

    /** Fully live, standard limits */
    PRODUCTION;

    public boolean isLive() {
        return this == PILOT || this == PRODUCTION;
    }
}
