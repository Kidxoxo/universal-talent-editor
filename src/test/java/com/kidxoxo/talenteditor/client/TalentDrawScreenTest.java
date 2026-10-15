package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TalentDrawScreenTest {
    @Test
    void showsRarityPathsAndLiveProbabilitiesWithoutZeroWeightEntries() {
        Map<ResourceLocation, Double> probabilities = new LinkedHashMap<>();
        probabilities.put(new ResourceLocation("ripples_talents", "r"), .645);
        probabilities.put(new ResourceLocation("ripples_talents", "sr"), .25);
        probabilities.put(new ResourceLocation("ripples_talents", "ssr"), .105);
        probabilities.put(new ResourceLocation("ripples_talents", "ur"), 0.0);
        assertEquals("r 64.5% · sr 25.0% · ssr 10.5%", TalentDrawScreen.probabilitySummary(probabilities));
        assertEquals("-", TalentDrawScreen.probabilitySummary(Map.of()));
        assertEquals("-", TalentDrawScreen.probabilitySummary(Map.of(new ResourceLocation("ripples_talents", "ur"), 0.0)));
    }

    @Test
    void keepsConfiguredIconSizeWhenTheCardHasRoom() {
        assertEquals(48, TalentDrawScreen.fittedIconSize(48, 120, 140));
    }

    @Test
    void shrinksIconsToFitNarrowOrShortCards() {
        assertEquals(32, TalentDrawScreen.fittedIconSize(64, 40, 140));
        assertEquals(19, TalentDrawScreen.fittedIconSize(64, 120, 80));
    }
}
