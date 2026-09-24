package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.DamageOperation;
import com.kidxoxo.talenteditor.domain.ModifyDamageAction;
import com.kidxoxo.talenteditor.domain.ModifyDamageExpressionAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class ModifyDamageExpressionActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "operation", "expression", "minimum", "maximum");
    @Override public ResourceLocation id() { return ModifyDamageExpressionAction.TYPE; }
    @Override public Set<ActionPlacement> supportedPlacements() { return Set.of(ActionPlacement.TRIGGER); }

    @Override public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        DamageOperation operation;
        try {
            operation = DamageOperation.parse(JsonAccess.string(
                    json, "operation", "add", false, context.source(), context.path(), context.report()
            ));
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_DAMAGE_OPERATION", context.source(),
                    JsonAccess.child(context.path(), "operation"), "Expected add or multiply");
            operation = DamageOperation.ADD;
        }
        double minimum = JsonAccess.decimal(json, "minimum", 0.0D, 0.0D, ModifyDamageAction.MAX_DAMAGE,
                context.source(), context.path(), context.report());
        double maximum = JsonAccess.decimal(json, "maximum", ModifyDamageAction.MAX_DAMAGE,
                0.0D, ModifyDamageAction.MAX_DAMAGE, context.source(), context.path(), context.report());
        if (minimum > maximum) {
            context.report().error("DAMAGE_BOUNDS_ORDER", context.source(), context.path(),
                    "minimum must not exceed maximum");
            maximum = minimum;
        }
        return Optional.of(new ModifyDamageExpressionAction(
                operation, ExpressionDecoderSupport.expression(context, json, "expression", "0"), minimum, maximum
        ));
    }
}
