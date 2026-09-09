package com.kidxoxo.talenteditor.runtime;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TalentAttributeRuntimeTest {
    @Test
    void modifierIdsAreStableAndSeparatedByTalentAttributeAndKey() {
        ResourceLocation talent = new ResourceLocation("example", "warrior");
        ResourceLocation health = new ResourceLocation("minecraft", "generic.max_health");
        ResourceLocation armor = new ResourceLocation("minecraft", "generic.armor");

        UUID original = TalentAttributeRuntime.modifierId(talent, health, "primary");
        assertEquals(original, TalentAttributeRuntime.modifierId(talent, health, "primary"));
        assertNotEquals(original, TalentAttributeRuntime.modifierId(talent, armor, "primary"));
        assertNotEquals(original, TalentAttributeRuntime.modifierId(talent, health, "secondary"));
        assertNotEquals(original, TalentAttributeRuntime.modifierId(
                new ResourceLocation("example", "mage"), health, "primary"
        ));
    }

    @Test
    void percentageSourcesAreClamped() {
        assertEquals(0.0D, TalentAttributeRuntime.clamp01(-0.2D));
        assertEquals(0.35D, TalentAttributeRuntime.clamp01(0.35D));
        assertEquals(1.0D, TalentAttributeRuntime.clamp01(1.4D));
    }
}
