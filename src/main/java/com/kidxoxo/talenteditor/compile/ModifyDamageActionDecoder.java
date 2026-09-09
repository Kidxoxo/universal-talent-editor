package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.DamageOperation;
import com.kidxoxo.talenteditor.domain.ModifyDamageAction;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

final class ModifyDamageActionDecoder implements ActionDecoder {
    private static final Set<String> FIELDS = Set.of("type", "operation", "amount", "minimum", "maximum");

    @Override
    public ResourceLocation id() {
        return ModifyDamageAction.TYPE;
    }

    @Override
    public Set<ActionPlacement> supportedPlacements() {
        return Set.of(ActionPlacement.TRIGGER);
    }

    @Override
    public Optional<? extends CompiledAction> decode(ActionDecodeContext context, JsonObject json) {
        JsonAccess.knownKeys(json, FIELDS, context.source(), context.path(), context.report());
        String rawOperation = JsonAccess.string(
                json, "operation", "add", false, context.source(), context.path(), context.report()
        );
        DamageOperation operation;
        try {
            operation = DamageOperation.parse(rawOperation);
        } catch (IllegalArgumentException exception) {
            context.report().error("INVALID_DAMAGE_OPERATION", context.source(),
                    JsonAccess.child(context.path(), "operation"), "Expected add or multiply");
            operation = DamageOperation.ADD;
        }
        double amount = JsonAccess.decimal(
                json, "amount", operation == DamageOperation.MULTIPLY ? 1.0D : 0.0D,
                -ModifyDamageAction.MAX_DAMAGE, ModifyDamageAction.MAX_DAMAGE,
                context.source(), context.path(), context.report()
        );
        if (operation == DamageOperation.MULTIPLY && (amount < 0.0D || amount > 1_000.0D)) {
            context.report().error("DAMAGE_MULTIPLIER_RANGE", context.source(),
                    JsonAccess.child(context.path(), "amount"), "Damage multiplier must be in range 0..1000");
            amount = 1.0D;
        }
        double minimum = JsonAccess.decimal(
                json, "minimum", 0.0D, 0.0D, ModifyDamageAction.MAX_DAMAGE,
                context.source(), context.path(), context.report()
        );
        double maximum = JsonAccess.decimal(
                json, "maximum", ModifyDamageAction.MAX_DAMAGE, 0.0D, ModifyDamageAction.MAX_DAMAGE,
                context.source(), context.path(), context.report()
        );
        if (minimum > maximum) {
            context.report().error("DAMAGE_BOUNDS_ORDER", context.source(), context.path(),
                    "minimum must not exceed maximum");
            maximum = minimum;
        }
        return Optional.of(new ModifyDamageAction(operation, amount, minimum, maximum));
    }
}
