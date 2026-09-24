package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ModifyAttributeBaseExpressionAction(
        ResourceLocation attribute,
        StateOperation operation,
        NumericExpression expression,
        double minimum,
        double maximum,
        EntitySelectorSpec selector
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "modify_attribute_base_expression");

    public ModifyAttributeBaseExpressionAction {
        attribute = Objects.requireNonNull(attribute, "attribute");
        operation = Objects.requireNonNull(operation, "operation");
        expression = Objects.requireNonNull(expression, "expression");
        selector = Objects.requireNonNull(selector, "selector");
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("Invalid attribute bounds");
        }
    }

    @Override public ResourceLocation type() { return TYPE; }
}
