package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.List;

public record ConfiguredItemAttributeModifier(
        ResourceLocation attribute,
        double amount,
        AttributeOperation operation,
        List<ConfiguredItemSlot> slots,
        String modifierKey
) {
    public ConfiguredItemAttributeModifier {
        attribute = Objects.requireNonNull(attribute, "attribute");
        operation = Objects.requireNonNull(operation, "operation");
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        if (slots.isEmpty() || slots.stream().anyMatch(Objects::isNull)
                || slots.stream().distinct().count() != slots.size()) {
            throw new IllegalArgumentException("slots must be nonempty and unique");
        }
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("amount must be finite");
        if (modifierKey == null || modifierKey.isBlank()) {
            throw new IllegalArgumentException("modifierKey must not be blank");
        }
    }

    /** Legacy single-slot constructor, kept for existing callers and tests. */
    public ConfiguredItemAttributeModifier(ResourceLocation attribute, double amount,
                                           AttributeOperation operation, ConfiguredItemSlot slot,
                                           String modifierKey) {
        this(attribute, amount, operation, List.of(slot), modifierKey);
    }

    public ConfiguredItemSlot slot() {
        return slots.get(0);
    }

    /** Distinct IDs let two equipped copies contribute in different slots. */
    public String modifierIdentity(ConfiguredItemSlot slot) {
        return slots.size() == 1 ? modifierKey : modifierKey + "|" + slot.serializedName();
    }
}
