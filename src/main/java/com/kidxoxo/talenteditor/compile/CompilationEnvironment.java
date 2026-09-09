package com.kidxoxo.talenteditor.compile;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record CompilationEnvironment(
        Set<String> loadedMods,
        Set<ResourceLocation> registeredAttributes,
        Set<ResourceLocation> registeredItems,
        Set<ResourceLocation> registeredEntityTypes,
        Set<ResourceLocation> registeredMobEffects,
        Set<ResourceLocation> registeredBlocks,
        Set<ResourceLocation> registeredSoundEvents
) {
    public CompilationEnvironment {
        LinkedHashSet<String> normalizedMods = new LinkedHashSet<>();
        for (String mod : loadedMods) {
            normalizedMods.add(mod.toLowerCase(Locale.ROOT));
        }
        loadedMods = Set.copyOf(normalizedMods);
        registeredAttributes = Set.copyOf(registeredAttributes);
        registeredItems = Set.copyOf(registeredItems);
        registeredEntityTypes = Set.copyOf(registeredEntityTypes);
        registeredMobEffects = Set.copyOf(registeredMobEffects);
        registeredBlocks = Set.copyOf(registeredBlocks);
        registeredSoundEvents = Set.copyOf(registeredSoundEvents);
    }

    public CompilationEnvironment(
            Set<String> loadedMods,
            Set<ResourceLocation> registeredAttributes,
            Set<ResourceLocation> registeredItems,
            Set<ResourceLocation> registeredEntityTypes,
            Set<ResourceLocation> registeredMobEffects,
            Set<ResourceLocation> registeredBlocks
    ) {
        this(loadedMods, registeredAttributes, registeredItems, registeredEntityTypes,
                registeredMobEffects, registeredBlocks, Set.of());
    }

    public CompilationEnvironment(
            Set<String> loadedMods,
            Set<ResourceLocation> registeredAttributes,
            Set<ResourceLocation> registeredItems,
            Set<ResourceLocation> registeredEntityTypes,
            Set<ResourceLocation> registeredMobEffects
    ) {
        this(loadedMods, registeredAttributes, registeredItems, registeredEntityTypes,
                registeredMobEffects, Set.of(), Set.of());
    }

    public CompilationEnvironment(
            Set<String> loadedMods,
            Set<ResourceLocation> registeredAttributes,
            Set<ResourceLocation> registeredItems,
            Set<ResourceLocation> registeredEntityTypes
    ) {
        this(loadedMods, registeredAttributes, registeredItems, registeredEntityTypes,
                Set.of(), Set.of(), Set.of());
    }

    public CompilationEnvironment(
            Set<String> loadedMods,
            Set<ResourceLocation> registeredAttributes,
            Set<ResourceLocation> registeredItems
    ) {
        this(loadedMods, registeredAttributes, registeredItems, Set.of(), Set.of(), Set.of(), Set.of());
    }

    public CompilationEnvironment(Set<String> loadedMods, Set<ResourceLocation> registeredAttributes) {
        this(loadedMods, registeredAttributes, Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }

    public boolean isModLoaded(String modId) {
        return loadedMods.contains(modId.toLowerCase(Locale.ROOT));
    }

    public boolean hasAttribute(ResourceLocation id) {
        return registeredAttributes.contains(id);
    }

    public boolean hasItem(ResourceLocation id) {
        return registeredItems.contains(id);
    }

    public boolean hasEntityType(ResourceLocation id) {
        return registeredEntityTypes.contains(id);
    }

    public boolean hasMobEffect(ResourceLocation id) {
        return registeredMobEffects.contains(id);
    }

    public boolean hasBlock(ResourceLocation id) {
        return registeredBlocks.contains(id);
    }

    public boolean hasSoundEvent(ResourceLocation id) {
        return registeredSoundEvents.contains(id);
    }

    public static CompilationEnvironment empty() {
        return new CompilationEnvironment(Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
    }
}
