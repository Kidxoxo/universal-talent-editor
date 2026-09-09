package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TalentDefinition(
        ResourceLocation id,
        int schema,
        boolean enabled,
        LocalizedText name,
        List<LocalizedText> description,
        boolean showGeneratedEffects,
        ResourceLocation rarity,
        List<ResourceLocation> categories,
        Optional<ResourceLocation> icon,
        OfferSettings offer,
        int capacityCost,
        List<ResourceLocation> tags,
        List<String> requiredMods,
        boolean available,
        List<String> missingMods,
        CompiledCondition acquireCondition,
        SynthesisDefinition synthesis,
        List<CompiledAction> activeEffects,
        List<CompiledTrigger> triggers,
        List<CompiledAction> onAcquire,
        List<CompiledAction> onSynthesize,
        List<CompiledAction> onRemove
) {
    public TalentDefinition {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        description = List.copyOf(description);
        rarity = Objects.requireNonNull(rarity, "rarity");
        categories = List.copyOf(categories);
        icon = Objects.requireNonNull(icon, "icon");
        offer = Objects.requireNonNull(offer, "offer");
        tags = List.copyOf(tags);
        requiredMods = List.copyOf(requiredMods);
        missingMods = List.copyOf(missingMods);
        acquireCondition = Objects.requireNonNull(acquireCondition, "acquireCondition");
        synthesis = Objects.requireNonNull(synthesis, "synthesis");
        activeEffects = List.copyOf(activeEffects);
        triggers = List.copyOf(triggers);
        onAcquire = List.copyOf(onAcquire);
        onSynthesize = List.copyOf(onSynthesize);
        onRemove = List.copyOf(onRemove);
    }
}
