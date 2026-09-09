package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.RemoveEffectAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class RemoveEffectActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "effect", "target");

    @Override
    public ResourceLocation id() {
        return RemoveEffectAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ITEM_USE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation effect = EffectActionDecoderSupport.effect(context, json);
        ActionTarget target = EffectActionDecoderSupport.target(context, json);
        return Optional.of(new RemoveEffectAction(effect, target));
    }
}
