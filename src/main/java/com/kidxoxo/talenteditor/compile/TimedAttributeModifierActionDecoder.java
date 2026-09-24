package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.AttributeOperation;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.TimedAttributeModifierAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class TimedAttributeModifierActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "attribute", "expression", "operation", "modifierKey", "durationTicks",
            "target", "radius", "relation", "order", "limit"
    );
    @Override public ResourceLocation id() { return TimedAttributeModifierAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.TRIGGER); }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation attribute = JsonAccess.requiredId(json, "attribute", "minecraft:generic.movement_speed", true,
                context.source(), context.path(), context.report());
        if (!context.environment().hasAttribute(attribute)) {
            context.report().error("UNKNOWN_ATTRIBUTE", context.source(), JsonAccess.child(context.path(), "attribute"),
                    "Attribute is not registered in the current server environment: " + attribute);
        }
        AttributeOperation operation;
        try {
            operation = AttributeOperation.parse(JsonAccess.string(
                    json, "operation", "addition", false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_ATTRIBUTE_OPERATION", context.source(),
                    JsonAccess.child(context.path(), "operation"),
                    "Expected addition, multiply_base, or multiply_total");
            operation = AttributeOperation.ADDITION;
        }
        String modifierKey = JsonAccess.string(json, "modifierKey", context.defaultModifierKey(), false,
                context.source(), context.path(), context.report());
        if (!modifierKey.matches("[a-z0-9_.-]{1,160}")) {
            context.report().error("INVALID_MODIFIER_KEY", context.source(),
                    JsonAccess.child(context.path(), "modifierKey"),
                    "modifierKey must use 1-160 lowercase letters, digits, dot, underscore, or dash");
            modifierKey = context.defaultModifierKey();
        }
        int duration = JsonAccess.integer(json, "durationTicks", 100, 1, 1_728_000,
                context.source(), context.path(), context.report());
        return Optional.of(new TimedAttributeModifierAction(
                attribute, ExpressionDecoderSupport.expression(context, json, "expression", "0"),
                operation, modifierKey, duration, EntitySelectorDecoderSupport.selector(context, json)
        ));
    }
}
