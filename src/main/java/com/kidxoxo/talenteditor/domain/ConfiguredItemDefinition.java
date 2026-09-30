package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

public record ConfiguredItemDefinition(
        ResourceLocation id,
        int schema,
        boolean enabled,
        LocalizedText name,
        List<LocalizedText> description,
        int customModelData,
        boolean glint,
        List<ConfiguredItemAttributeModifier> attributeModifiers,
        ConfiguredItemUseDefinition use,
        List<CompiledTrigger> triggers,
        List<ConfiguredItemLootRule> loot,
        List<String> requiredMods,
        boolean available,
        List<String> missingMods,
        boolean curiosEquippable,
        boolean mainhandOnly,
        List<String> curiosSlots
) {
    public ConfiguredItemDefinition {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        description = List.copyOf(description);
        attributeModifiers = List.copyOf(attributeModifiers);
        use = Objects.requireNonNull(use, "use");
        triggers = List.copyOf(triggers);
        loot = List.copyOf(loot);
        if (customModelData < 0 || customModelData > 16_777_215) {
            throw new IllegalArgumentException("customModelData must be in range 0..16777215");
        }
        requiredMods = List.copyOf(requiredMods);
        missingMods = List.copyOf(missingMods);
        curiosSlots = List.copyOf(curiosSlots);
    }

    public boolean allowsCuriosSlot(String slot) {
        return curiosEquippable && !mainhandOnly
                && (curiosSlots.isEmpty() || slot != null && curiosSlots.contains(slot));
    }

    public ConfiguredItemDefinition(
            ResourceLocation id, int schema, boolean enabled, LocalizedText name,
            List<LocalizedText> description, int customModelData, boolean glint,
            List<ConfiguredItemAttributeModifier> attributeModifiers, ConfiguredItemUseDefinition use,
            List<CompiledTrigger> triggers, List<ConfiguredItemLootRule> loot,
            List<String> requiredMods, boolean available, List<String> missingMods
    ) {
        this(id, schema, enabled, name, description, customModelData, glint, attributeModifiers, use,
                triggers, loot, requiredMods, available, missingMods, true, false, List.of());
    }

    public ConfiguredItemDefinition(
            ResourceLocation id, int schema, boolean enabled, LocalizedText name,
            List<LocalizedText> description, int customModelData, boolean glint,
            List<ConfiguredItemAttributeModifier> attributeModifiers, ConfiguredItemUseDefinition use,
            List<CompiledTrigger> triggers, List<ConfiguredItemLootRule> loot,
            List<String> requiredMods, boolean available, List<String> missingMods,
            boolean curiosEquippable, boolean mainhandOnly
    ) {
        this(id, schema, enabled, name, description, customModelData, glint, attributeModifiers, use,
                triggers, loot, requiredMods, available, missingMods,
                curiosEquippable, mainhandOnly, List.of());
    }

    public ConfiguredItemDefinition(
            ResourceLocation id,
            int schema,
            boolean enabled,
            LocalizedText name,
            List<LocalizedText> description,
            int customModelData,
            boolean glint,
            List<ConfiguredItemAttributeModifier> attributeModifiers,
            ConfiguredItemUseDefinition use,
            List<ConfiguredItemLootRule> loot,
            List<String> requiredMods,
            boolean available,
            List<String> missingMods
    ) {
        this(id, schema, enabled, name, description, customModelData, glint, attributeModifiers, use,
                List.of(), loot, requiredMods, available, missingMods, true, false, List.of());
    }
}
