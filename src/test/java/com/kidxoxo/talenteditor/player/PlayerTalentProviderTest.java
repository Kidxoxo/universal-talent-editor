package com.kidxoxo.talenteditor.player;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.LazyOptional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTalentProviderTest {
    @Test
    void recreatesInvalidatedOptionalWithoutClearingDataWhenForgeRevivesPlayer() {
        PlayerTalentProvider provider = new PlayerTalentProvider();
        LazyOptional<PlayerTalentData> first = provider.talentData();
        PlayerTalentData data = first.resolve().orElseThrow();
        ResourceLocation talent = new ResourceLocation("example", "persistent");
        data.grant(talent);
        data.setDrawTokens(7);

        provider.invalidate();
        assertFalse(first.isPresent());

        LazyOptional<PlayerTalentData> revived = provider.talentData();
        PlayerTalentData restored = revived.resolve().orElseThrow();
        assertTrue(revived.isPresent());
        assertSame(data, restored);
        assertTrue(restored.contains(talent));
        assertEquals(7, restored.drawTokens());

        provider.invalidate();
        assertFalse(revived.isPresent());
    }
}
