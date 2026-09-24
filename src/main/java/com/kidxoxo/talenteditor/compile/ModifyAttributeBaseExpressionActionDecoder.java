package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ModifyAttributeBaseExpressionAction;
import com.kidxoxo.talenteditor.domain.StateOperation;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class ModifyAttributeBaseExpressionActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "attribute", "operation", "expression", "minimum", "maximum",
            "target", "radius", "relation", "order", "limit"
    );
    @Override public ResourceLocation id() { return ModifyAttributeBaseExpressionAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER, ActionPlacement.ON_ACQUIRE);
    }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation attribute = JsonAccess.requiredId(json, "attribute", "minecraft:generic.max_health", true,
                context.source(), context.path(), context.report());
        if (!context.environment().hasAttribute(attribute)) {
            context.report().error("UNKNOWN_ATTRIBUTE", context.source(), JsonAccess.child(context.path(), "attribute"),
                    "Attribute is not registered in the current server environment: " + attribute);
        }
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
        double minimum = JsonAccess.decimal(json, "minimum", -1.0E9D, -1.0E9D, 1.0E9D,
                context.source(), context.path(), context.report());
        double maximum = JsonAccess.decimal(json, "maximum", 1.0E9D, -1.0E9D, 1.0E9D,
                context.source(), context.path(), context.report());
        if (minimum > maximum) {
            context.report().error("ATTRIBUTE_BOUNDS_ORDER", context.source(), context.path(),
                    "minimum must not exceed maximum");
            maximum = minimum;
        }
        return Optional.of(new ModifyAttributeBaseExpressionAction(
                attribute, operation, ExpressionDecoderSupport.expression(context, json, "expression", "0"),
                minimum, maximum, EntitySelectorDecoderSupport.selector(context, json)
        ));
    }
}
