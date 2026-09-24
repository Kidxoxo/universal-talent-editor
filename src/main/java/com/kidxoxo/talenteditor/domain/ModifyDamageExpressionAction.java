package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ModifyDamageExpressionAction(
        DamageOperation operation,
        NumericExpression expression,
        double minimum,
        double maximum
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "modify_damage_expression");

    public ModifyDamageExpressionAction {
        operation = Objects.requireNonNull(operation, "operation");
        expression = Objects.requireNonNull(expression, "expression");
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)
                || minimum < 0.0D || maximum > ModifyDamageAction.MAX_DAMAGE || minimum > maximum) {
            throw new IllegalArgumentException("Invalid damage bounds");
        }
    }

    public float apply(float current, double evaluated) {
        double changed = operation == DamageOperation.ADD ? current + evaluated : current * evaluated;
        return (float) Math.max(minimum, Math.min(maximum, changed));
    }

    @Override public ResourceLocation type() { return TYPE; }
}
