package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record OfferSettings(
        boolean enabled,
        double weight,
        List<ResourceLocation> pools
) {
    public OfferSettings {
        pools = List.copyOf(pools);
    }

    public static OfferSettings disabled() {
        return new OfferSettings(false, 0.0D, List.of());
    }
}

