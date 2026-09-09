package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.DamageOperation;
import com.kidxoxo.talenteditor.domain.LivingEntityTarget;
import com.kidxoxo.talenteditor.domain.ModifyEntityAttributeAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class ModifyEntityAttributeActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of(
            "type", "attribute", "operation", "amount", "minimum", "maximum", "target", "radius"
    );

    @Override
    public ResourceLocation id() {
        return ModifyEntityAttributeAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        ResourceLocation attribute = JsonAccess.requiredId(
                json, "attribute", "minecraft:generic.movement_speed", true,
                context.source(), context.path(), context.report()
        );
        if (!context.environment().hasAttribute(attribute)) {
            context.report().error("UNKNOWN_ATTRIBUTE", context.source(),
                    JsonAccess.child(context.path(), "attribute"),
                    "Attribute is not registered in the current server environment: " + attribute);
        }
        DamageOperation operation;
        try {
            operation = DamageOperation.parse(JsonAccess.string(
                    json, "operation", "add", false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_ATTRIBUTE_OPERATION", context.source(),
                    JsonAccess.child(context.path(), "operation"), "Expected add or multiply");
            operation = DamageOperation.ADD;
        }
        double amount = JsonAccess.decimal(
                json, "amount", operation == DamageOperation.MULTIPLY ? 1.0D : 0.0D,
                -ModifyEntityAttributeAction.LIMIT, ModifyEntityAttributeAction.LIMIT,
                context.source(), context.path(), context.report()
        );
        if (operation == DamageOperation.MULTIPLY && (amount < 0.0D || amount > 1_000.0D)) {
            context.report().error("ATTRIBUTE_MULTIPLIER_RANGE", context.source(),
                    JsonAccess.child(context.path(), "amount"), "Attribute multiplier must be 0..1000");
            amount = 1.0D;
        }
        double minimum = JsonAccess.decimal(
                json, "minimum", -ModifyEntityAttributeAction.LIMIT,
                -ModifyEntityAttributeAction.LIMIT, ModifyEntityAttributeAction.LIMIT,
                context.source(), context.path(), context.report()
        );
        double maximum = JsonAccess.decimal(
                json, "maximum", ModifyEntityAttributeAction.LIMIT,
                -ModifyEntityAttributeAction.LIMIT, ModifyEntityAttributeAction.LIMIT,
                context.source(), context.path(), context.report()
        );
        if (minimum > maximum) {
            context.report().error("ATTRIBUTE_BOUNDS_ORDER", context.source(), context.path(),
                    "minimum must not exceed maximum");
            maximum = minimum;
        }
        LivingEntityTarget target = EntityActionDecoderSupport.target(context, json);
        double radius = EntityActionDecoderSupport.radius(context, json);
        return Optional.of(new ModifyEntityAttributeAction(
                attribute, operation, amount, minimum, maximum, target, radius
        ));
    }
}
