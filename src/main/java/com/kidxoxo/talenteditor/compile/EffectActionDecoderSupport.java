package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.ActionTarget;
import net.minecraft.resources.ResourceLocation;

final class EffectActionDecoderSupport {
    private EffectActionDecoderSupport() {
    }

    static ResourceLocation effect(ActionDecodeContext context, JsonObject json) {
        ResourceLocation effect = JsonAccess.requiredId(
                json, "effect", "minecraft:speed", true,
                context.source(), context.path(), context.report()
        );
        if (!context.environment().hasMobEffect(effect)) {
            if (context.missingMods().isEmpty()) {
                context.report().error("UNKNOWN_MOB_EFFECT", context.source(),
                        JsonAccess.child(context.path(), "effect"), "Unknown mob effect '" + effect + "'");
            } else {
                context.report().warning("MOB_EFFECT_UNAVAILABLE_WITH_MISSING_MOD", context.source(),
                        JsonAccess.child(context.path(), "effect"),
                        "Mob effect is unavailable while required mods are missing: " + effect);
            }
        }
        return effect;
    }

    static ActionTarget target(ActionDecodeContext context, JsonObject json) {
        String rawTarget = JsonAccess.string(
                json, "target", "self", false, context.source(), context.path(), context.report()
        );
        try {
            return ActionTarget.parse(rawTarget);
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_ACTION_TARGET", context.source(),
                    JsonAccess.child(context.path(), "target"), "Expected self or event_entity");
            return ActionTarget.SELF;
        }
    }
}
