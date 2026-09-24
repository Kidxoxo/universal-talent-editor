package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.EntityOrder;
import com.kidxoxo.talenteditor.domain.EntityRelation;
import com.kidxoxo.talenteditor.domain.EntitySelectorSpec;
import com.kidxoxo.talenteditor.domain.LivingEntityTarget;

final class EntitySelectorDecoderSupport {
    private EntitySelectorDecoderSupport() {
    }

    static EntitySelectorSpec selector(ActionDecodeContext context, JsonObject json) {
        String defaultTarget = context.placement() == ActionPlacement.TRIGGER ? "event_entity" : "self";
        LivingEntityTarget target;
        EntityRelation relation;
        EntityOrder order;
        try {
            target = LivingEntityTarget.parse(JsonAccess.string(
                    json, "target", defaultTarget, false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_ENTITY_ACTION_TARGET", context.source(),
                    JsonAccess.child(context.path(), "target"),
                    "Expected self, event_entity, damage_source, or nearby");
            target = LivingEntityTarget.SELF;
        }
        try {
            relation = EntityRelation.parse(JsonAccess.string(
                    json, "relation", "any", false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_ENTITY_RELATION", context.source(),
                    JsonAccess.child(context.path(), "relation"), "Expected any, ally, or enemy");
            relation = EntityRelation.ANY;
        }
        try {
            order = EntityOrder.parse(JsonAccess.string(
                    json, "order", "nearest", false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_ENTITY_ORDER", context.source(),
                    JsonAccess.child(context.path(), "order"), "Expected nearest, farthest, or random");
            order = EntityOrder.NEAREST;
        }
        double radius = JsonAccess.decimal(
                json, "radius", 4.0D, 0.5D, 64.0D,
                context.source(), context.path(), context.report()
        );
        int limit = JsonAccess.integer(
                json, "limit", 64, 1, 256, context.source(), context.path(), context.report()
        );
        if (target != LivingEntityTarget.NEARBY && (json.has("relation") || json.has("order") || json.has("limit"))) {
            context.report().warning("SELECTOR_OPTION_IGNORED", context.source(), context.path(),
                    "relation, order, and limit only affect nearby targets");
        }
        return new EntitySelectorSpec(target, radius, relation, order, limit);
    }
}
