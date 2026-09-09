package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.AttributeOperation;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeModifierAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeSource;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class DynamicAttributeModifierActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "attribute", "source", "counter", "base", "scale", "minimum", "maximum",
            "operation", "modifierKey", "showInDescription"
    );

    @Override
    public ResourceLocation id() {
        return DynamicAttributeModifierAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.ACTIVE);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation attribute = JsonAccess.requiredId(
                json, "attribute", "minecraft:generic.attack_damage", true,
                context.source(), context.path(), context.report()
        );
        String sourceName = JsonAccess.string(
                json, "source", "counter", false, context.source(), context.path(), context.report()
        );
        DynamicAttributeSource source = DynamicAttributeSource.parse(sourceName).orElseGet(() -> {
            context.report().error(
                    "INVALID_DYNAMIC_ATTRIBUTE_SOURCE", context.source(),
                    JsonAccess.child(context.path(), "source"),
                    "Expected counter, health_percent, or hunger_percent"
            );
            return DynamicAttributeSource.COUNTER;
        });
        ResourceLocation counter = json.has("counter") ? JsonAccess.requiredId(
                json, "counter", "talent_editor:counter", true,
                context.source(), context.path(), context.report()
        ) : null;
        if (source == DynamicAttributeSource.COUNTER && counter == null) {
            context.report().error(
                    "DYNAMIC_COUNTER_REQUIRED", context.source(), JsonAccess.child(context.path(), "counter"),
                    "A counter source requires a counter id"
            );
            counter = new ResourceLocation("talent_editor", "invalid_counter");
        } else if (source != DynamicAttributeSource.COUNTER && counter != null) {
            context.report().error(
                    "DYNAMIC_COUNTER_NOT_ALLOWED", context.source(), JsonAccess.child(context.path(), "counter"),
                    "counter is only available when source is counter"
            );
            counter = null;
        }
        double base = number(json, "base", 0.0D, context);
        double scale = number(json, "scale", 1.0D, context);
        double minimum = number(json, "minimum", -1.0E12D, context);
        double maximum = number(json, "maximum", 1.0E12D, context);
        if (minimum > maximum) {
            context.report().error(
                    "DYNAMIC_ATTRIBUTE_RANGE", context.source(), JsonAccess.child(context.path(), "maximum"),
                    "maximum must be greater than or equal to minimum"
            );
            maximum = minimum;
        }
        String operationName = JsonAccess.string(
                json, "operation", "addition", false, context.source(), context.path(), context.report()
        );
        AttributeOperation operation;
        try {
            operation = AttributeOperation.parse(operationName);
        } catch (IllegalArgumentException exception) {
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
                json, "showInDescription", true, context.source(), context.path(), context.report()
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
        ResourceLocation maxHealth = new ResourceLocation("minecraft", "generic.max_health");
        if (source == DynamicAttributeSource.HEALTH_PERCENT && attribute.equals(maxHealth)) {
            context.report().error(
                    "DYNAMIC_ATTRIBUTE_SELF_REFERENCE", context.source(), context.path(),
                    "health_percent cannot modify minecraft:generic.max_health because the formula would be recursive"
            );
        }
        return Optional.of(new DynamicAttributeModifierAction(
                attribute, source, Optional.ofNullable(counter), base, scale, minimum, maximum,
                operation, modifierKey, show
        ));
    }

    private static double number(JsonObject json, String key, double fallback, ActionDecodeContext context) {
        return JsonAccess.decimal(
                json, key, fallback, -1.0E12D, 1.0E12D,
                context.source(), context.path(), context.report()
        );
    }
}
