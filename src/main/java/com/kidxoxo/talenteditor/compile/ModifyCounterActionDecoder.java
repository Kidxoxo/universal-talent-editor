package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ModifyCounterAction;
import com.kidxoxo.talenteditor.domain.StateOperation;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class ModifyCounterActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "counter", "operation", "expression", "minimum", "maximum"
    );
    @Override public ResourceLocation id() { return ModifyCounterAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE,
                ActionPlacement.ON_SYNTHESIZE, ActionPlacement.ON_REMOVE);
    }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation counter = JsonAccess.requiredId(
                json, "counter", "talent_editor:counter", true,
                context.source(), context.path(), context.report()
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
        long minimum = JsonAccess.longInteger(
                json, "minimum", 0L, 0L, 1_000_000_000_000L,
                context.source(), context.path(), context.report()
        );
        long maximum = JsonAccess.longInteger(
                json, "maximum", 1_000_000_000_000L, 0L, 1_000_000_000_000L,
                context.source(), context.path(), context.report()
        );
        if (minimum > maximum) {
            context.report().error("COUNTER_BOUNDS_ORDER", context.source(), context.path(),
                    "minimum must not exceed maximum");
            maximum = minimum;
        }
        return Optional.of(new ModifyCounterAction(
                counter, operation, ExpressionDecoderSupport.expression(context, json, "expression", "1"),
                minimum, maximum
        ));
    }
}
