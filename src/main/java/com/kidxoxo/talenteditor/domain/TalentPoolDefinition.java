package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TalentPoolDefinition(
        ResourceLocation id,
        int schema,
        boolean enabled,
        LocalizedText name,
        int choices,
        int refreshes,
        int maxOwned,
        int iconSize,
        Map<ResourceLocation, Double> rarityWeights,
        Optional<ResourceLocation> luckAttribute,
        Map<ResourceLocation, Double> luckFactors,
        Optional<ResourceLocation> pityRarity,
        int pityAfter,
        DuplicatePolicy duplicatePolicy,
        Optional<ResourceLocation> duplicateCounter,
        long duplicateCounterAmount
) {
    public TalentPoolDefinition {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        rarityWeights = Map.copyOf(new LinkedHashMap<>(rarityWeights));
        luckAttribute = luckAttribute == null ? Optional.empty() : luckAttribute;
        luckFactors = Map.copyOf(new LinkedHashMap<>(luckFactors));
        pityRarity = pityRarity == null ? Optional.empty() : pityRarity;
        duplicatePolicy = Objects.requireNonNull(duplicatePolicy, "duplicatePolicy");
        duplicateCounter = duplicateCounter == null ? Optional.empty() : duplicateCounter;
    }

    public double rarityWeight(RarityDefinition rarity) {
        return rarityWeights.getOrDefault(rarity.id(), rarity.baseWeight());
    }

    public boolean excludeOwned() {
        return duplicatePolicy == DuplicatePolicy.EXCLUDE;
    }

    public double luckFactor(ResourceLocation rarity) {
        return luckFactors.getOrDefault(rarity, 0.0D);
    }
}
