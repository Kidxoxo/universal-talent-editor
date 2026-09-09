package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ConfiguredItemAttributeModifier(
        ResourceLocation attribute,
        double amount,
        AttributeOperation operation,
        ConfiguredItemSlot slot,
        String modifierKey
) {
    public ConfiguredItemAttributeModifier {
        attribute = Objects.requireNonNull(attribute, "attribute");
        operation = Objects.requireNonNull(operation, "operation");
        slot = Objects.requireNonNull(slot, "slot");
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("amount must be finite");
        if (modifierKey == null || modifierKey.isBlank()) {
            throw new IllegalArgumentException("modifierKey must not be blank");
        }
    }
}
