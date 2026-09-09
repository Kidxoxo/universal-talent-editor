package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SynthesisDefinition(
        boolean enabled,
        List<ResourceLocation> consumeTalents,
        Map<ResourceLocation, Long> consumeCounters,
        Map<ResourceLocation, Integer> consumeItems
) {
    public SynthesisDefinition {
        consumeTalents = List.copyOf(consumeTalents);
        consumeCounters = Collections.unmodifiableMap(new LinkedHashMap<>(consumeCounters));
        consumeItems = Collections.unmodifiableMap(new LinkedHashMap<>(consumeItems));
    }

    public static SynthesisDefinition disabled() {
        return new SynthesisDefinition(false, List.of(), Map.of(), Map.of());
    }
}
