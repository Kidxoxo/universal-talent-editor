package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.LivingEntityTarget;

final class EntityActionDecoderSupport {
    private EntityActionDecoderSupport() {
    }

    static LivingEntityTarget target(ActionDecodeContext context, JsonObject json) {
        try {
            return LivingEntityTarget.parse(JsonAccess.string(
                    json, "target", "event_entity", false,
                    context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_ENTITY_ACTION_TARGET", context.source(),
                    JsonAccess.child(context.path(), "target"),
                    "Expected self, event_entity, damage_source, or nearby");
            return LivingEntityTarget.EVENT_ENTITY;
        }
    }

    static double radius(ActionDecodeContext context, JsonObject json) {
        return JsonAccess.decimal(
                json, "radius", 4.0D, 0.5D, 64.0D,
                context.source(), context.path(), context.report()
        );
    }
}
