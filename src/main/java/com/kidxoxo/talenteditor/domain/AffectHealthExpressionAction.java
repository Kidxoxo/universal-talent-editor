package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record AffectHealthExpressionAction(
        HealthOperation operation,
        NumericExpression expression,
        double maximum,
        boolean invertNegative,
        EntitySelectorSpec selector
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "affect_health_expression");

    public AffectHealthExpressionAction {
        operation = Objects.requireNonNull(operation, "operation");
        expression = Objects.requireNonNull(expression, "expression");
        selector = Objects.requireNonNull(selector, "selector");
        if (!Double.isFinite(maximum) || maximum < 0.0D || maximum > 1.0E9D) {
            throw new IllegalArgumentException("maximum must be 0..1e9");
        }
    }

    public HealthOperation resolvedOperation(double evaluated) {
        if (evaluated < 0.0D && invertNegative) {
            return operation == HealthOperation.DAMAGE ? HealthOperation.HEAL : HealthOperation.DAMAGE;
        }
        return operation;
    }

    public double boundedMagnitude(double evaluated) {
        double magnitude = evaluated < 0.0D && invertNegative ? -evaluated : evaluated;
        return Double.isFinite(magnitude) ? Math.max(0.0D, Math.min(maximum, magnitude)) : 0.0D;
    }

    @Override public ResourceLocation type() { return TYPE; }
}
