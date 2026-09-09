package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

public record DynamicAttributeModifierAction(
        ResourceLocation attribute,
        DynamicAttributeSource source,
        Optional<ResourceLocation> counter,
        double base,
        double scale,
        double minimum,
        double maximum,
        AttributeOperation operation,
        String modifierKey,
        boolean showInDescription
) implements CompiledAction {
    public static final ResourceLocation TYPE =
            new ResourceLocation(TalentEditorMod.MOD_ID, "dynamic_attribute_modifier");

    public DynamicAttributeModifierAction {
        attribute = Objects.requireNonNull(attribute, "attribute");
        source = Objects.requireNonNull(source, "source");
        counter = counter == null ? Optional.empty() : counter;
        operation = Objects.requireNonNull(operation, "operation");
        if (source == DynamicAttributeSource.COUNTER && counter.isEmpty()) {
            throw new IllegalArgumentException("counter source requires a counter id");
        }
        if (source != DynamicAttributeSource.COUNTER && counter.isPresent()) {
            throw new IllegalArgumentException("counter id is only valid for the counter source");
        }
        if (!Double.isFinite(base) || !Double.isFinite(scale)
                || !Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("dynamic formula bounds must be finite and ordered");
        }
        if (modifierKey == null || modifierKey.isBlank()) {
            throw new IllegalArgumentException("modifierKey must not be blank");
        }
    }

    public double amount(double sourceValue) {
        double value = base + sourceValue * scale;
        if (!Double.isFinite(value)) value = value < 0.0D ? minimum : maximum;
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
