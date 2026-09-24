package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record TimedAttributeModifierAction(
        ResourceLocation attribute,
        NumericExpression expression,
        AttributeOperation operation,
        String modifierKey,
        int durationTicks,
        EntitySelectorSpec selector
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "timed_attribute_modifier");

    public TimedAttributeModifierAction {
        attribute = Objects.requireNonNull(attribute, "attribute");
        expression = Objects.requireNonNull(expression, "expression");
        operation = Objects.requireNonNull(operation, "operation");
        modifierKey = Objects.requireNonNull(modifierKey, "modifierKey");
        selector = Objects.requireNonNull(selector, "selector");
        if (!modifierKey.matches("[a-z0-9_.-]{1,160}")) throw new IllegalArgumentException("Invalid modifier key");
        if (durationTicks < 1 || durationTicks > 1_728_000) throw new IllegalArgumentException("durationTicks must be 1..1728000");
    }

    @Override public ResourceLocation type() { return TYPE; }
}
