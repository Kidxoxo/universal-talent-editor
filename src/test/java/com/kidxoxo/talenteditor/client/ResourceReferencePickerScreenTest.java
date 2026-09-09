package com.kidxoxo.talenteditor.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceReferencePickerScreenTest {
    @Test
    void searchesResourceIdAndLocalizedChineseName() {
        ResourceLocation id = new ResourceLocation("minecraft", "generic.attack_damage");

        assertTrue(ResourceReferencePickerScreen.matchesSearch(id, "攻击伤害", "攻击"));
        assertTrue(ResourceReferencePickerScreen.matchesSearch(id, "攻击伤害", "GENERIC.ATTACK"));
        assertTrue(ResourceReferencePickerScreen.matchesSearch(id, "攻击伤害", "  minecraft:generic  "));
        assertTrue(ResourceReferencePickerScreen.matchesSearch(id, "攻击伤害", ""));
        assertFalse(ResourceReferencePickerScreen.matchesSearch(id, "攻击伤害", "移动速度"));
    }
}
