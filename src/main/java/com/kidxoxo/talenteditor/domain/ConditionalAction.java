package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/** Conditions attached to one action, without changing its placement or payload. */
public record ConditionalAction(
        CompiledAction delegate,
        Optional<CompiledCondition> enabledWhen,
        Optional<CompiledCondition> disabledWhen
) implements CompiledAction {
    public ConditionalAction {
        delegate = Objects.requireNonNull(delegate, "delegate");
        enabledWhen = Objects.requireNonNull(enabledWhen, "enabledWhen");
        disabledWhen = Objects.requireNonNull(disabledWhen, "disabledWhen");
        if (delegate instanceof ConditionalAction) throw new IllegalArgumentException("Nested action condition");
    }

    @Override public ResourceLocation type() { return delegate.type(); }

    public static CompiledAction unwrap(CompiledAction action) {
        return action instanceof ConditionalAction conditional ? conditional.delegate() : action;
    }
}
