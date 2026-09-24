package com.kidxoxo.talenteditor.domain;

/** Optional, data-driven disappearance policy for a talent. Zero means disabled. */
public record TalentLifecycleDefinition(
        int durationTicks,
        int removeAfterDraws,
        boolean retainEffects,
        boolean retainCategoryContribution,
        boolean retainTriggers
) {
    public TalentLifecycleDefinition {
        if (durationTicks < 0 || durationTicks > 172_800_000) {
            throw new IllegalArgumentException("durationTicks must be 0..172800000");
        }
        if (removeAfterDraws < 0 || removeAfterDraws > 1_000_000) {
            throw new IllegalArgumentException("removeAfterDraws must be 0..1000000");
        }
    }

    public static TalentLifecycleDefinition permanent() {
        return new TalentLifecycleDefinition(0, 0, false, false, false);
    }

    public boolean temporary() {
        return durationTicks > 0 || removeAfterDraws > 0;
    }
}
