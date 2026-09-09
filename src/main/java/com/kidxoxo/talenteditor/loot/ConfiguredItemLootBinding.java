package com.kidxoxo.talenteditor.loot;

import com.kidxoxo.talenteditor.domain.ConfiguredItemLootRule;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ConfiguredItemLootBinding(
        ResourceLocation itemId,
        ConfiguredItemLootRule rule
) {
    public ConfiguredItemLootBinding {
        itemId = Objects.requireNonNull(itemId, "itemId");
        rule = Objects.requireNonNull(rule, "rule");
    }
}
