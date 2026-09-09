package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Queues a one-shot filter for a player's next explicit draw refresh.
 * Values inside one selector list are ORed; non-empty selector groups are ANDed.
 */
public record QueueDrawConstraintAction(
        List<ResourceLocation> talents,
        List<ResourceLocation> categories,
        List<ResourceLocation> rarities,
        List<ResourceLocation> tags,
        List<ResourceLocation> pools,
        int guaranteed,
        DrawConstraintQueueMode queueMode,
        Optional<ResourceLocation> successCounter,
        long successIncrement,
        int milestoneEvery,
        Optional<DrawConstraintSpec> milestone
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(
            TalentEditorMod.MOD_ID, "queue_draw_constraint"
    );

    public QueueDrawConstraintAction {
        talents = List.copyOf(talents);
        categories = List.copyOf(categories);
        rarities = List.copyOf(rarities);
        tags = List.copyOf(tags);
        pools = List.copyOf(pools);
        new DrawConstraintSpec(talents, categories, rarities, tags, pools, guaranteed);
        queueMode = queueMode == null ? DrawConstraintQueueMode.APPEND : queueMode;
        successCounter = successCounter == null ? Optional.empty() : successCounter;
        milestone = milestone == null ? Optional.empty() : milestone;
        if (successCounter.isEmpty() && successIncrement != 0L) {
            throw new IllegalArgumentException("successIncrement requires successCounter");
        }
        if (successCounter.isPresent() && (successIncrement < 1L || successIncrement > 1_000_000_000_000L)) {
            throw new IllegalArgumentException("successIncrement must be in range 1..1000000000000");
        }
        if (milestone.isPresent() != (milestoneEvery > 0)) {
            throw new IllegalArgumentException("milestone and milestoneEvery must be configured together");
        }
        if (milestone.isPresent() && successCounter.isEmpty()) {
            throw new IllegalArgumentException("milestone requires successCounter");
        }
        if (milestoneEvery < 0 || milestoneEvery > 1_000_000_000) {
            throw new IllegalArgumentException("milestoneEvery must be in range 0..1000000000");
        }
    }

    public QueueDrawConstraintAction(
            List<ResourceLocation> talents,
            List<ResourceLocation> categories,
            List<ResourceLocation> rarities,
            List<ResourceLocation> tags,
            List<ResourceLocation> pools,
            int guaranteed
    ) {
        this(talents, categories, rarities, tags, pools, guaranteed,
                DrawConstraintQueueMode.APPEND, Optional.empty(), 0L, 0, Optional.empty());
    }

    public boolean appliesTo(ResourceLocation pool) {
        return spec().appliesTo(pool);
    }

    public boolean matches(TalentDefinition talent) {
        return spec().matches(talent);
    }

    public DrawConstraintSpec spec() {
        return new DrawConstraintSpec(talents, categories, rarities, tags, pools, guaranteed);
    }

    public long nextSuccessCount(long current) {
        if (successCounter.isEmpty()) return current;
        if (current >= 1_000_000_000_000L - successIncrement) return 1_000_000_000_000L;
        return current + successIncrement;
    }

    public DrawConstraintSpec constraintForSuccess(long previous, long next) {
        if (milestone.isPresent() && next / milestoneEvery > previous / milestoneEvery) {
            return milestone.orElseThrow();
        }
        return spec();
    }

    public static QueueDrawConstraintAction pending(DrawConstraintSpec spec) {
        return new QueueDrawConstraintAction(
                spec.talents(), spec.categories(), spec.rarities(), spec.tags(), spec.pools(), spec.guaranteed()
        );
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
