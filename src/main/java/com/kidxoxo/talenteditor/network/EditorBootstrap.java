package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.catalog.AttributeDescriptor;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record EditorBootstrap(
        long revision,
        String contentHash,
        List<RaritySummary> rarities,
        List<TalentSummary> talents,
        List<PoolSummary> pools,
        List<CounterSummary> counters,
        List<LayoutSummary> layouts,
        List<DefinitionReference> references,
        boolean referencesTruncated,
        List<ResourceLocation> entityTypeTags,
        List<ResourceLocation> itemTags,
        List<ResourceLocation> damageTypeTags,
        List<AttributeDescriptor> attributes,
        List<ConfiguredItemSummary> configuredItems
) {
    public EditorBootstrap {
        rarities = List.copyOf(rarities);
        talents = List.copyOf(talents);
        pools = List.copyOf(pools);
        counters = List.copyOf(counters);
        layouts = List.copyOf(layouts);
        references = List.copyOf(references);
        entityTypeTags = List.copyOf(entityTypeTags);
        itemTags = List.copyOf(itemTags);
        damageTypeTags = List.copyOf(damageTypeTags);
        attributes = List.copyOf(attributes);
        configuredItems = List.copyOf(configuredItems);
    }

    public EditorBootstrap(
            long revision,
            String contentHash,
            List<RaritySummary> rarities,
            List<TalentSummary> talents,
            List<PoolSummary> pools,
            List<CounterSummary> counters,
            List<LayoutSummary> layouts,
            List<DefinitionReference> references,
            boolean referencesTruncated,
            List<ResourceLocation> entityTypeTags,
            List<ResourceLocation> itemTags,
            List<ResourceLocation> damageTypeTags,
            List<AttributeDescriptor> attributes
    ) {
        this(revision, contentHash, rarities, talents, pools, counters, layouts, references,
                referencesTruncated, entityTypeTags, itemTags, damageTypeTags, attributes, List.of());
    }

    public EditorBootstrap(
            long revision,
            String contentHash,
            List<RaritySummary> rarities,
            List<TalentSummary> talents,
            List<PoolSummary> pools,
            List<CounterSummary> counters,
            List<DefinitionReference> references,
            boolean referencesTruncated,
            List<ResourceLocation> entityTypeTags,
            List<ResourceLocation> itemTags,
            List<ResourceLocation> damageTypeTags,
            List<AttributeDescriptor> attributes
    ) {
        this(revision, contentHash, rarities, talents, pools, counters, List.of(), references,
                referencesTruncated, entityTypeTags, itemTags, damageTypeTags, attributes);
    }

    public EditorBootstrap(
            long revision,
            String contentHash,
            List<RaritySummary> rarities,
            List<TalentSummary> talents,
            List<PoolSummary> pools,
            List<CounterSummary> counters,
            List<ResourceLocation> entityTypeTags,
            List<ResourceLocation> itemTags,
            List<ResourceLocation> damageTypeTags,
            List<AttributeDescriptor> attributes
    ) {
        this(revision, contentHash, rarities, talents, pools, counters, List.of(), List.of(), false,
                entityTypeTags, itemTags, damageTypeTags, attributes);
    }

    public EditorBootstrap(
            long revision,
            String contentHash,
            List<RaritySummary> rarities,
            List<TalentSummary> talents,
            List<PoolSummary> pools,
            List<CounterSummary> counters,
            List<AttributeDescriptor> attributes
    ) {
        this(revision, contentHash, rarities, talents, pools, counters, List.of(), List.of(), false,
                List.of(), List.of(), List.of(), attributes);
    }

    public EditorBootstrap(
            long revision,
            String contentHash,
            List<RaritySummary> rarities,
            List<TalentSummary> talents,
            List<PoolSummary> pools,
            List<AttributeDescriptor> attributes
    ) {
        this(revision, contentHash, rarities, talents, pools, List.of(), List.of(), List.of(), false,
                List.of(), List.of(), List.of(), attributes);
    }

    public record RaritySummary(
            ResourceLocation id,
            String name,
            int color,
            int order,
            boolean enabled,
            boolean offerable,
            double baseWeight,
            String source,
            int inboundReferences
    ) {
        public RaritySummary {
            source = normalizeSource(source);
            inboundReferences = Math.max(0, inboundReferences);
        }

        public RaritySummary(
                ResourceLocation id, String name, int color, int order,
                boolean enabled, boolean offerable, double baseWeight
        ) {
            this(id, name, color, order, enabled, offerable, baseWeight, "unknown", 0);
        }

        public RaritySummary(
                ResourceLocation id, String name, int color, int order,
                boolean enabled, boolean offerable, double baseWeight, String source
        ) {
            this(id, name, color, order, enabled, offerable, baseWeight, source, 0);
        }
    }

    public record TalentSummary(
            ResourceLocation id,
            String name,
            ResourceLocation rarity,
            boolean enabled,
            boolean available,
            List<ResourceLocation> categories,
            String source,
            int inboundReferences
    ) {
        public TalentSummary {
            categories = List.copyOf(categories);
            source = normalizeSource(source);
            inboundReferences = Math.max(0, inboundReferences);
        }

        public TalentSummary(
                ResourceLocation id, String name, ResourceLocation rarity, boolean enabled, boolean available
        ) {
            this(id, name, rarity, enabled, available, List.of(), "unknown", 0);
        }

        public TalentSummary(
                ResourceLocation id, String name, ResourceLocation rarity, boolean enabled, boolean available,
                List<ResourceLocation> categories
        ) {
            this(id, name, rarity, enabled, available, categories, "unknown", 0);
        }

        public TalentSummary(
                ResourceLocation id, String name, ResourceLocation rarity, boolean enabled, boolean available,
                List<ResourceLocation> categories, String source
        ) {
            this(id, name, rarity, enabled, available, categories, source, 0);
        }
    }

    public record PoolSummary(
            ResourceLocation id,
            String name,
            boolean enabled,
            int choices,
            int refreshes,
            int maxOwned,
            boolean excludeOwned,
            Map<ResourceLocation, Double> rarityWeights,
            String source,
            int inboundReferences
    ) {
        public PoolSummary {
            rarityWeights = Map.copyOf(new LinkedHashMap<>(rarityWeights));
            source = normalizeSource(source);
            inboundReferences = Math.max(0, inboundReferences);
        }

        public PoolSummary(
                ResourceLocation id, String name, boolean enabled, int choices, int refreshes,
                int maxOwned, boolean excludeOwned, Map<ResourceLocation, Double> rarityWeights
        ) {
            this(id, name, enabled, choices, refreshes, maxOwned, excludeOwned, rarityWeights, "unknown", 0);
        }

        public PoolSummary(
                ResourceLocation id, String name, boolean enabled, int choices, int refreshes,
                int maxOwned, boolean excludeOwned, Map<ResourceLocation, Double> rarityWeights, String source
        ) {
            this(id, name, enabled, choices, refreshes, maxOwned, excludeOwned, rarityWeights, source, 0);
        }
    }

    public record CounterSummary(
            ResourceLocation id,
            String name,
            String event,
            boolean enabled,
            boolean available,
            List<ResourceLocation> categories,
            String visibility,
            String source,
            int inboundReferences
    ) {
        public CounterSummary {
            categories = List.copyOf(categories);
            source = normalizeSource(source);
            inboundReferences = Math.max(0, inboundReferences);
        }

        public CounterSummary(
                ResourceLocation id, String name, String event, boolean enabled, boolean available
        ) {
            this(id, name, event, enabled, available, List.of(), "full", "unknown", 0);
        }

        public CounterSummary(
                ResourceLocation id, String name, String event, boolean enabled, boolean available,
                List<ResourceLocation> categories, String visibility
        ) {
            this(id, name, event, enabled, available, categories, visibility, "unknown", 0);
        }

        public CounterSummary(
                ResourceLocation id, String name, String event, boolean enabled, boolean available,
                List<ResourceLocation> categories, String visibility, String source
        ) {
            this(id, name, event, enabled, available, categories, visibility, source, 0);
        }
    }

    public record LayoutSummary(
            ResourceLocation id,
            boolean enabled,
            boolean active,
            int priority,
            Optional<ResourceLocation> background,
            int panelPercent,
            int columns,
            int rows,
            int slotWidth,
            int slotHeight,
            int iconSize,
            int freeSlotCount,
            int pageCount,
            boolean showGeneratedEffects,
            String source,
            int inboundReferences
    ) {
        public LayoutSummary {
            background = background == null ? Optional.empty() : background;
            freeSlotCount = Math.max(0, freeSlotCount);
            pageCount = Math.max(0, pageCount);
            source = normalizeSource(source);
            inboundReferences = Math.max(0, inboundReferences);
        }
    }

    public record ConfiguredItemSummary(
            ResourceLocation id,
            String name,
            boolean enabled,
            boolean available,
            int customModelData,
            boolean glint,
            String source,
            int inboundReferences
    ) {
        public ConfiguredItemSummary {
            source = normalizeSource(source);
            inboundReferences = Math.max(0, inboundReferences);
        }
    }

    private static String normalizeSource(String source) {
        return source == null || source.isBlank() ? "unknown" : source;
    }
}
