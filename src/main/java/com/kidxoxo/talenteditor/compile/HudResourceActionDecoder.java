package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.HudResourceAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class HudResourceActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "state", "maximum", "color", "order", "showWhenFull");
    @Override public ResourceLocation id() { return HudResourceAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.ACTIVE); }
    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation state = JsonAccess.requiredId(json, "state", "talent_editor:resource", true,
                context.source(), context.path(), context.report());
        int color = JsonAccess.integer(json, "color", 0x55AAFF, 0, 0xFFFFFF,
                context.source(), context.path(), context.report());
        int order = JsonAccess.integer(json, "order", 0, -10_000, 10_000,
                context.source(), context.path(), context.report());
        return Optional.of(new HudResourceAction(state,
                ExpressionDecoderSupport.expression(context, json, "maximum", "100"), color, order,
                JsonAccess.bool(json, "showWhenFull", true, context.source(), context.path(), context.report())));
    }
}
