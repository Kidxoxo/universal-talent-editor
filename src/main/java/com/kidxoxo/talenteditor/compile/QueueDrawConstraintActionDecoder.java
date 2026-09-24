package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.DrawConstraintQueueMode;
import com.kidxoxo.talenteditor.domain.DrawConstraintSpec;
import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

final class QueueDrawConstraintActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "talents", "categories", "rarities", "tags", "pools", "guaranteed",
            "queueMode", "successCounter", "successIncrement", "milestoneEvery", "milestone"
    );
    private static final Set<String> SPEC_FIELDS = Set.of(
            "talents", "categories", "rarities", "tags", "pools", "guaranteed"
    );
    private static final int MAX_SELECTOR_VALUES = 256;

    @Override
    public ResourceLocation id() {
        return QueueDrawConstraintAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        List<ResourceLocation> talents = ids(context, json, "talents");
        List<ResourceLocation> categories = ids(context, json, "categories");
        List<ResourceLocation> rarities = ids(context, json, "rarities");
        List<ResourceLocation> tags = ids(context, json, "tags");
        List<ResourceLocation> pools = ids(context, json, "pools");
        if (talents.isEmpty() && categories.isEmpty() && rarities.isEmpty() && tags.isEmpty()) {
            context.report().error("DRAW_CONSTRAINT_TARGET_REQUIRED", context.source(), context.path(),
                    "At least one talent, category, rarity, or tag selector is required");
            return Optional.empty();
        }
        int guaranteed = JsonAccess.integer(
                json, "guaranteed", 1, 1, 5, context.source(), context.path(), context.report()
        );
        String rawQueueMode = JsonAccess.string(
                json, "queueMode", "append", false, context.source(), context.path(), context.report()
        );
        DrawConstraintQueueMode queueMode = DrawConstraintQueueMode.parse(rawQueueMode).orElseGet(() -> {
            context.report().error("UNKNOWN_DRAW_QUEUE_MODE", context.source(),
                    JsonAccess.child(context.path(), "queueMode"),
                    "Unknown draw constraint queue mode '" + rawQueueMode + "'");
            return DrawConstraintQueueMode.APPEND;
        });
        ResourceLocation successCounterValue = JsonAccess.optionalId(
                json, "successCounter", true, context.source(), context.path(), context.report()
        );
        Optional<ResourceLocation> successCounter = Optional.ofNullable(successCounterValue);
        long successIncrement = JsonAccess.longInteger(
                json, "successIncrement", successCounter.isPresent() ? 1L : 0L,
                successCounter.isPresent() ? 1L : 0L,
                successCounter.isPresent() ? 1_000_000_000_000L : 0L,
                context.source(), context.path(), context.report()
        );
        if (successCounter.isEmpty() && (json.has("successIncrement") || json.has("milestoneEvery")
                || json.has("milestone"))) {
            context.report().error("DRAW_SUCCESS_COUNTER_REQUIRED", context.source(), context.path(),
                    "successIncrement and milestone settings require successCounter");
        }
        int milestoneEvery = JsonAccess.integer(
                json, "milestoneEvery", json.has("milestone") ? 1 : 0,
                json.has("milestone") ? 1 : 0, 1_000_000_000,
                context.source(), context.path(), context.report()
        );
        Optional<DrawConstraintSpec> milestone = Optional.empty();
        if (json.has("milestone")) {
            JsonObject milestoneJson = JsonAccess.object(
                    json, "milestone", true, context.source(), context.path(), context.report()
            );
            String milestonePath = JsonAccess.child(context.path(), "milestone");
            JsonAccess.knownKeys(milestoneJson, SPEC_FIELDS, context.source(), milestonePath, context.report());
            milestone = spec(context, milestoneJson, milestonePath, pools, guaranteed);
            if (milestone.isEmpty()) milestoneEvery = 0;
        } else if (json.has("milestoneEvery")) {
            context.report().error("DRAW_MILESTONE_REQUIRED", context.source(),
                    JsonAccess.child(context.path(), "milestoneEvery"),
                    "milestoneEvery requires a milestone selector object");
            milestoneEvery = 0;
        }
        if (successCounter.isEmpty()) {
            successIncrement = 0L;
            milestoneEvery = 0;
            milestone = Optional.empty();
        }
        return Optional.of(new QueueDrawConstraintAction(
                talents, categories, rarities, tags, pools, guaranteed,
                queueMode, successCounter, successIncrement, milestoneEvery, milestone
        ));
    }

    private static Optional<DrawConstraintSpec> spec(
            ActionDecodeContext context,
            JsonObject json,
            String path,
            List<ResourceLocation> inheritedPools,
            int inheritedGuaranteed
    ) {
        List<ResourceLocation> talents = ids(context, json, "talents", path);
        List<ResourceLocation> categories = ids(context, json, "categories", path);
        List<ResourceLocation> rarities = ids(context, json, "rarities", path);
        List<ResourceLocation> tags = ids(context, json, "tags", path);
        if (talents.isEmpty() && categories.isEmpty() && rarities.isEmpty() && tags.isEmpty()) {
            context.report().error("DRAW_CONSTRAINT_TARGET_REQUIRED", context.source(), path,
                    "At least one talent, category, rarity, or tag selector is required");
            return Optional.empty();
        }
        List<ResourceLocation> pools = json.has("pools") ? ids(context, json, "pools", path) : inheritedPools;
        int guaranteed = JsonAccess.integer(
                json, "guaranteed", inheritedGuaranteed, 1, 5, context.source(), path, context.report()
        );
        return Optional.of(new DrawConstraintSpec(talents, categories, rarities, tags, pools, guaranteed));
    }

    private static List<ResourceLocation> ids(
            ActionDecodeContext context,
            JsonObject json,
            String key
    ) {
        return ids(context, json, key, context.path());
    }

    private static List<ResourceLocation> ids(
            ActionDecodeContext context,
            JsonObject json,
            String key,
            String path
    ) {
        List<ResourceLocation> values = JsonAccess.idList(
                json, key, true, context.source(), path, context.report()
        );
        if (values.size() > MAX_SELECTOR_VALUES) {
            context.report().error("LIST_TOO_LARGE", context.source(),
                    JsonAccess.child(path, key),
                    "At most " + MAX_SELECTOR_VALUES + " values are allowed");
            return values.subList(0, MAX_SELECTOR_VALUES);
        }
        return values;
    }
}
