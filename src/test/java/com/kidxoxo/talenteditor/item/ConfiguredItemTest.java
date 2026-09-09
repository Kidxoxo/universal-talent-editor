package com.kidxoxo.talenteditor.item;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConfiguredItemTest {
    @Test
    void modifierIdsAreStableAndSeparatedByLogicalItemAndKey() {
        ResourceLocation first = new ResourceLocation("example", "first");
        ResourceLocation second = new ResourceLocation("example", "second");

        assertEquals(ConfiguredItemModifierIds.create(first, "damage"),
                ConfiguredItemModifierIds.create(first, "damage"));
        assertNotEquals(ConfiguredItemModifierIds.create(first, "damage"),
                ConfiguredItemModifierIds.create(first, "speed"));
        assertNotEquals(ConfiguredItemModifierIds.create(first, "damage"),
                ConfiguredItemModifierIds.create(second, "damage"));
    }
}
