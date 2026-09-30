package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.PresentationTransformAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class PresentationTransformActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "atTick", "durationTicks", "alphaFrom", "alphaTo",
            "scaleFrom", "scaleTo", "offsetX", "offsetY", "offsetZ", "positionMode");

    @Override public ResourceLocation id() { return PresentationTransformAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.ON_ACQUIRE, ActionPlacement.ON_SYNTHESIZE);
    }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        int at = JsonAccess.integer(json, "atTick", 0, 0, 2400, context.source(), context.path(), context.report());
        int duration = JsonAccess.integer(json, "durationTicks", 40, 2, 200,
                context.source(), context.path(), context.report());
        double alphaFrom = JsonAccess.decimal(json, "alphaFrom", 1, 0, 1,
                context.source(), context.path(), context.report());
        double alphaTo = JsonAccess.decimal(json, "alphaTo", 1, 0, 1,
                context.source(), context.path(), context.report());
        double scaleFrom = JsonAccess.decimal(json, "scaleFrom", 1, 0.1, 4,
                context.source(), context.path(), context.report());
        double scaleTo = JsonAccess.decimal(json, "scaleTo", 1, 0.1, 4,
                context.source(), context.path(), context.report());
        double x = JsonAccess.decimal(json, "offsetX", 0, -3, 3,
                context.source(), context.path(), context.report());
        double y = JsonAccess.decimal(json, "offsetY", 0, -3, 3,
                context.source(), context.path(), context.report());
        double z = JsonAccess.decimal(json, "offsetZ", 0, -3, 3,
                context.source(), context.path(), context.report());
        String mode = JsonAccess.string(json, "positionMode", "return", false,
                context.source(), context.path(), context.report());
        if (!Set.of("return", "hold").contains(mode)) context.report().error(
                "INVALID_PRESENTATION_POSITION_MODE", context.source(),
                JsonAccess.child(context.path(), "positionMode"), "Expected return or hold");
        return Optional.of(new PresentationTransformAction(at, duration, alphaFrom, alphaTo,
                scaleFrom, scaleTo, x, y, z, mode));
    }
}
