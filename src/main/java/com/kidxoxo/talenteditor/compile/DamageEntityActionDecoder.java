package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.DamageEntityAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class DamageEntityActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "target", "amount", "radius");

    @Override public ResourceLocation id() { return DamageEntityAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.TRIGGER); }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        float amount = (float) JsonAccess.decimal(
                json, "amount", 2.0D, 0.000001D, 1_000_000.0D,
                context.source(), context.path(), context.report()
        );
        return Optional.of(new DamageEntityAction(
                EntityActionDecoderSupport.target(context, json), amount,
                EntityActionDecoderSupport.radius(context, json)
        ));
    }
}
