package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class AddEffectActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "effect", "durationTicks", "amplifier", "target", "ambient", "showParticles", "showIcon"
    );

    @Override
    public ResourceLocation id() {
        return AddEffectAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.ACTIVE, ActionPlacement.TRIGGER, ActionPlacement.ITEM_USE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation effect = EffectActionDecoderSupport.effect(context, json);
        int durationTicks = JsonAccess.integer(
                json, "durationTicks", 100, 1, 72_000, context.source(), context.path(), context.report()
        );
        int amplifier = JsonAccess.integer(
                json, "amplifier", 0, 0, 255, context.source(), context.path(), context.report()
        );
        ActionTarget target = EffectActionDecoderSupport.target(context, json);
        if (context.placement() == ActionPlacement.ACTIVE && target != ActionTarget.SELF) {
            context.report().error("ACTIVE_EFFECT_TARGET_UNSUPPORTED", context.source(),
                    JsonAccess.child(context.path(), "target"),
                    "A maintained active effect can only target self");
        }
        return Optional.of(new AddEffectAction(
                effect, durationTicks, amplifier, target,
                JsonAccess.bool(json, "ambient", false, context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "showParticles", true, context.source(), context.path(), context.report()),
                JsonAccess.bool(json, "showIcon", true, context.source(), context.path(), context.report())
        ));
    }
}
