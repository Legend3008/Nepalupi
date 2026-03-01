package np.com.nepalupi.domain.enums;

/**
 * PSP tiers — determines transaction limits.
 * <p>
 * Tier promotion based on 90-day performance data.
 */
public enum PspTier {

    /** New PSP, conservative limits (Rs 10K per txn, Rs 1L daily) */
    TIER_1(1, 1000000L, 10000000L),

    /** Proven PSP, standard limits (Rs 50K per txn, Rs 5L daily) */
    TIER_2(2, 5000000L, 50000000L),

    /** Premium PSP — large bank, high volume (Rs 1L per txn, Rs 10L daily) */
    TIER_3(3, 10000000L, 100000000L);

    private final int level;
    private final long perTxnLimitPaisa;
    private final long dailyLimitPaisa;

    PspTier(int level, long perTxnLimitPaisa, long dailyLimitPaisa) {
        this.level = level;
        this.perTxnLimitPaisa = perTxnLimitPaisa;
        this.dailyLimitPaisa = dailyLimitPaisa;
    }

    public int getLevel() { return level; }
    public long getPerTxnLimitPaisa() { return perTxnLimitPaisa; }
    public long getDailyLimitPaisa() { return dailyLimitPaisa; }

    public static PspTier fromLevel(int level) {
        for (PspTier tier : values()) {
            if (tier.level == level) return tier;
        }
        return TIER_1;
    }
}
