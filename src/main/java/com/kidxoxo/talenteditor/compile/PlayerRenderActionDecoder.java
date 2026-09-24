package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.PlayerRenderAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class PlayerRenderActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "alpha", "scale", "hideNameplate");
    @Override public ResourceLocation id() { return PlayerRenderAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.ACTIVE); }
    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        var alpha = ExpressionDecoderSupport.expression(context, json, "alpha", "1");
        var scale = ExpressionDecoderSupport.expression(context, json, "scale", "1");
        return Optional.of(new PlayerRenderAction(alpha, scale,
                JsonAccess.bool(json, "hideNameplate", false, context.source(), context.path(), context.report())));
    }
}
