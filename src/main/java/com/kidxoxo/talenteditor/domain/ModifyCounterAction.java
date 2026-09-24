package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ModifyCounterAction(
        ResourceLocation counter,
        StateOperation operation,
        NumericExpression expression,
        long minimum,
        long maximum
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "modify_counter");

    public ModifyCounterAction {
        counter = Objects.requireNonNull(counter, "counter");
        operation = Objects.requireNonNull(operation, "operation");
        expression = Objects.requireNonNull(expression, "expression");
        if (minimum < 0L || maximum > 1_000_000_000_000L || minimum > maximum) {
            throw new IllegalArgumentException("Invalid counter bounds");
        }
    }

    @Override public ResourceLocation type() { return TYPE; }
}
