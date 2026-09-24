package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ModifyNumericStateAction;
import com.kidxoxo.talenteditor.domain.StateOperation;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class ModifyNumericStateActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "state", "operation", "expression", "minimum", "maximum",
            "target", "radius", "relation", "order", "limit"
    );

    @Override public ResourceLocation id() { return ModifyNumericStateAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE,
                ActionPlacement.ON_SYNTHESIZE, ActionPlacement.ON_REMOVE);
    }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation state = JsonAccess.requiredId(
                json, "state", "talent_editor:value", true, context.source(), context.path(), context.report()
        );
        StateOperation operation;
        try {
            operation = StateOperation.parse(JsonAccess.string(
                    json, "operation", "add", false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_STATE_OPERATION", context.source(),
                    JsonAccess.child(context.path(), "operation"), "Expected set, add, or multiply");
            operation = StateOperation.ADD;
        }
        double minimum = JsonAccess.decimal(json, "minimum", -1.0E12D, -1.0E12D, 1.0E12D,
                context.source(), context.path(), context.report());
        double maximum = JsonAccess.decimal(json, "maximum", 1.0E12D, -1.0E12D, 1.0E12D,
                context.source(), context.path(), context.report());
        if (minimum > maximum) {
            context.report().error("STATE_BOUNDS_ORDER", context.source(), context.path(),
                    "minimum must not exceed maximum");
            maximum = minimum;
        }
        return Optional.of(new ModifyNumericStateAction(
                state, operation, ExpressionDecoderSupport.expression(context, json, "expression", "1"),
                minimum, maximum, EntitySelectorDecoderSupport.selector(context, json)
        ));
    }
}
