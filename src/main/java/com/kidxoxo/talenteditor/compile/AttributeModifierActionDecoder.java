package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.AttributeModifierAction;
import com.kidxoxo.talenteditor.domain.AttributeOperation;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class AttributeModifierActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "attribute", "amount", "operation", "modifierKey", "showInDescription"
    );
    private static final Set<ActionPlacement> PLACEMENTS = Set.of(ActionPlacement.ACTIVE);

    @Override
    public ResourceLocation id() {
        return AttributeModifierAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return PLACEMENTS;
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation attribute = JsonAccess.requiredId(
                json, "attribute", "minecraft:generic.max_health", true,
                context.source(), context.path(), context.report()
        );
        double amount = JsonAccess.decimal(
                json, "amount", 0.0D, -1.0E12D, 1.0E12D,
                context.source(), context.path(), context.report()
        );
        String operationName = JsonAccess.string(
                json, "operation", "addition", false,
                context.source(), context.path(), context.report()
        );
        AttributeOperation operation;
        try {
            operation = AttributeOperation.parse(operationName);
        } catch (IllegalArgumentException ex) {
            context.report().error(
                    "INVALID_ATTRIBUTE_OPERATION", context.source(), JsonAccess.child(context.path(), "operation"),
                    "Expected addition, multiply_base, or multiply_total"
            );
            operation = AttributeOperation.ADDITION;
        }
        String modifierKey = JsonAccess.string(
                json, "modifierKey", context.defaultModifierKey(), false,
                context.source(), context.path(), context.report()
        );
        if (!modifierKey.matches("[a-z0-9_.-]{1,160}")) {
            context.report().error(
                    "INVALID_MODIFIER_KEY", context.source(), JsonAccess.child(context.path(), "modifierKey"),
                    "modifierKey must use 1-160 lowercase letters, digits, dot, underscore, or dash"
            );
        }
        boolean show = JsonAccess.bool(
                json, "showInDescription", true,
                context.source(), context.path(), context.report()
        );

        if (!context.environment().hasAttribute(attribute)) {
            if (context.missingMods().isEmpty()) {
                context.report().error(
                        "UNKNOWN_ATTRIBUTE", context.source(), JsonAccess.child(context.path(), "attribute"),
                        "Attribute is not registered in the current server environment: " + attribute
                );
            } else {
                context.report().warning(
                        "ATTRIBUTE_UNAVAILABLE_WITH_MISSING_MOD", context.source(),
                        JsonAccess.child(context.path(), "attribute"),
                        "Attribute is unavailable while required mods are missing: " + attribute
                );
            }
        }

        return Optional.of(new AttributeModifierAction(attribute, amount, operation, modifierKey, show));
    }
}

