package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.catalog.AttributeDescriptor;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.domain.DefinitionTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

public record BootstrapPacket(EditorBootstrap bootstrap, boolean openScreen) {
    private static final int MAX_RARITIES = 2_048;
    private static final int MAX_TALENTS = 10_000;
    private static final int MAX_CATEGORIES_PER_TALENT = 256;
    private static final int MAX_POOLS = 2_048;
    private static final int MAX_COUNTERS = 4_096;
    private static final int MAX_LAYOUTS = 512;
    private static final int MAX_CONFIGURED_ITEMS = 10_000;
    private static final int MAX_CATEGORIES_PER_COUNTER = 256;
    private static final int MAX_RARITY_WEIGHTS = 256;
    private static final int MAX_ATTRIBUTES = 10_000;
    private static final int MAX_TAGS = 20_000;
    static final int MAX_REFERENCES = 50_000;
    private static final int MAX_TEXT = 512;
    private static final int MAX_USAGE = 64;
    private static final int MAX_PATH = 512;

    static void encode(BootstrapPacket packet, FriendlyByteBuf buffer) {
        EditorBootstrap bootstrap = packet.bootstrap();
        buffer.writeVarLong(bootstrap.revision());
        buffer.writeUtf(bootstrap.contentHash(), 64);
        buffer.writeVarInt(bootstrap.rarities().size());
        for (EditorBootstrap.RaritySummary rarity : bootstrap.rarities()) {
            buffer.writeResourceLocation(rarity.id());
            buffer.writeUtf(rarity.name(), MAX_TEXT);
            buffer.writeInt(rarity.color());
            buffer.writeVarInt(rarity.order());
            buffer.writeBoolean(rarity.enabled());
            buffer.writeBoolean(rarity.offerable());
            buffer.writeDouble(rarity.baseWeight());
            buffer.writeUtf(rarity.source(), MAX_TEXT);
            buffer.writeVarInt(rarity.inboundReferences());
        }
        buffer.writeVarInt(bootstrap.talents().size());
        for (EditorBootstrap.TalentSummary talent : bootstrap.talents()) {
            buffer.writeResourceLocation(talent.id());
            buffer.writeUtf(talent.name(), MAX_TEXT);
            buffer.writeResourceLocation(talent.rarity());
            buffer.writeBoolean(talent.enabled());
            buffer.writeBoolean(talent.available());
            if (talent.categories().size() > MAX_CATEGORIES_PER_TALENT) {
                throw new IllegalArgumentException("Talent category count exceeds " + MAX_CATEGORIES_PER_TALENT);
            }
            buffer.writeVarInt(talent.categories().size());
            talent.categories().forEach(buffer::writeResourceLocation);
            buffer.writeUtf(talent.source(), MAX_TEXT);
            buffer.writeVarInt(talent.inboundReferences());
        }
        buffer.writeVarInt(bootstrap.pools().size());
        for (EditorBootstrap.PoolSummary pool : bootstrap.pools()) {
            buffer.writeResourceLocation(pool.id());
            buffer.writeUtf(pool.name(), MAX_TEXT);
            buffer.writeBoolean(pool.enabled());
            buffer.writeVarInt(pool.choices());
            buffer.writeVarInt(pool.refreshes());
            buffer.writeVarInt(pool.maxOwned());
            buffer.writeBoolean(pool.excludeOwned());
            buffer.writeVarInt(pool.rarityWeights().size());
            pool.rarityWeights().forEach((rarity, weight) -> {
                buffer.writeResourceLocation(rarity);
                buffer.writeDouble(weight);
            });
            buffer.writeUtf(pool.source(), MAX_TEXT);
            buffer.writeVarInt(pool.inboundReferences());
        }
        buffer.writeVarInt(bootstrap.counters().size());
        for (EditorBootstrap.CounterSummary counter : bootstrap.counters()) {
            buffer.writeResourceLocation(counter.id());
            buffer.writeUtf(counter.name(), MAX_TEXT);
            buffer.writeUtf(counter.event(), 32);
            buffer.writeBoolean(counter.enabled());
            buffer.writeBoolean(counter.available());
            if (counter.categories().size() > MAX_CATEGORIES_PER_COUNTER) {
                throw new IllegalArgumentException(
                        "Counter category count exceeds " + MAX_CATEGORIES_PER_COUNTER
                );
            }
            buffer.writeVarInt(counter.categories().size());
            counter.categories().forEach(buffer::writeResourceLocation);
            buffer.writeUtf(counter.visibility(), 32);
            buffer.writeUtf(counter.source(), MAX_TEXT);
            buffer.writeVarInt(counter.inboundReferences());
        }
        if (bootstrap.layouts().size() > MAX_LAYOUTS) {
            throw new IllegalArgumentException("Layout count exceeds " + MAX_LAYOUTS);
        }
        buffer.writeVarInt(bootstrap.layouts().size());
        for (EditorBootstrap.LayoutSummary layout : bootstrap.layouts()) {
            buffer.writeResourceLocation(layout.id());
            buffer.writeBoolean(layout.enabled());
            buffer.writeBoolean(layout.active());
            buffer.writeVarInt(layout.priority());
            buffer.writeBoolean(layout.background().isPresent());
            layout.background().ifPresent(buffer::writeResourceLocation);
            buffer.writeVarInt(layout.panelPercent());
            buffer.writeVarInt(layout.columns());
            buffer.writeVarInt(layout.rows());
            buffer.writeVarInt(layout.slotWidth());
            buffer.writeVarInt(layout.slotHeight());
            buffer.writeVarInt(layout.iconSize());
            buffer.writeVarInt(layout.freeSlotCount());
            buffer.writeVarInt(layout.pageCount());
            buffer.writeBoolean(layout.showGeneratedEffects());
            buffer.writeUtf(layout.source(), MAX_TEXT);
            buffer.writeVarInt(layout.inboundReferences());
        }
        if (bootstrap.references().size() > MAX_REFERENCES) {
            throw new IllegalArgumentException("Definition reference count exceeds " + MAX_REFERENCES);
        }
        buffer.writeVarInt(bootstrap.references().size());
        for (DefinitionReference reference : bootstrap.references()) {
            buffer.writeEnum(reference.target().kind());
            buffer.writeResourceLocation(reference.target().id());
            buffer.writeEnum(reference.sourceKind());
            buffer.writeResourceLocation(reference.sourceId());
            buffer.writeUtf(reference.usage(), MAX_USAGE);
            buffer.writeUtf(reference.path(), MAX_PATH);
        }
        buffer.writeBoolean(bootstrap.referencesTruncated());
        writeIds(buffer, bootstrap.entityTypeTags(), "entity type tag");
        writeIds(buffer, bootstrap.itemTags(), "item tag");
        writeIds(buffer, bootstrap.damageTypeTags(), "damage type tag");
        buffer.writeVarInt(bootstrap.attributes().size());
        for (AttributeDescriptor attribute : bootstrap.attributes()) {
            buffer.writeResourceLocation(attribute.id());
            buffer.writeUtf(attribute.descriptionId(), MAX_TEXT);
            buffer.writeDouble(attribute.defaultValue());
            buffer.writeBoolean(attribute.minValue() != null);
            if (attribute.minValue() != null) {
                buffer.writeDouble(attribute.minValue());
                buffer.writeDouble(attribute.maxValue());
            }
            buffer.writeBoolean(attribute.clientSyncable());
            buffer.writeBoolean(attribute.attachedToTarget());
        }
        if (bootstrap.configuredItems().size() > MAX_CONFIGURED_ITEMS) {
            throw new IllegalArgumentException("Configured item count exceeds " + MAX_CONFIGURED_ITEMS);
        }
        buffer.writeVarInt(bootstrap.configuredItems().size());
        for (EditorBootstrap.ConfiguredItemSummary item : bootstrap.configuredItems()) {
            buffer.writeResourceLocation(item.id());
            buffer.writeUtf(item.name(), MAX_TEXT);
            buffer.writeBoolean(item.enabled());
            buffer.writeBoolean(item.available());
            buffer.writeVarInt(item.customModelData());
            buffer.writeBoolean(item.glint());
            buffer.writeUtf(item.source(), MAX_TEXT);
            buffer.writeVarInt(item.inboundReferences());
        }
        buffer.writeBoolean(packet.openScreen());
    }

    static BootstrapPacket decode(FriendlyByteBuf buffer) {
        long revision = buffer.readVarLong();
        String hash = buffer.readUtf(64);
        int rarityCount = boundedCount(buffer.readVarInt(), MAX_RARITIES, "rarity");
        ArrayList<EditorBootstrap.RaritySummary> rarities = new ArrayList<>(rarityCount);
        for (int index = 0; index < rarityCount; index++) {
            rarities.add(new EditorBootstrap.RaritySummary(
                    buffer.readResourceLocation(), buffer.readUtf(MAX_TEXT), buffer.readInt(),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readDouble(),
                    buffer.readUtf(MAX_TEXT), buffer.readVarInt()
            ));
        }
        int talentCount = boundedCount(buffer.readVarInt(), MAX_TALENTS, "talent");
        ArrayList<EditorBootstrap.TalentSummary> talents = new ArrayList<>(talentCount);
        for (int index = 0; index < talentCount; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            String name = buffer.readUtf(MAX_TEXT);
            ResourceLocation rarity = buffer.readResourceLocation();
            boolean enabled = buffer.readBoolean();
            boolean available = buffer.readBoolean();
            int categoryCount = boundedCount(
                    buffer.readVarInt(), MAX_CATEGORIES_PER_TALENT, "talent category"
            );
            ArrayList<ResourceLocation> categories = new ArrayList<>(categoryCount);
            for (int categoryIndex = 0; categoryIndex < categoryCount; categoryIndex++) {
                categories.add(buffer.readResourceLocation());
            }
            talents.add(new EditorBootstrap.TalentSummary(
                    id, name, rarity, enabled, available, categories,
                    buffer.readUtf(MAX_TEXT), buffer.readVarInt()
            ));
        }

        int poolCount = boundedCount(buffer.readVarInt(), MAX_POOLS, "pool");
        ArrayList<EditorBootstrap.PoolSummary> pools = new ArrayList<>(poolCount);
        for (int index = 0; index < poolCount; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            String name = buffer.readUtf(MAX_TEXT);
            boolean enabled = buffer.readBoolean();
            int choices = buffer.readVarInt();
            int refreshes = buffer.readVarInt();
            int maxOwned = buffer.readVarInt();
            boolean excludeOwned = buffer.readBoolean();
            int weightCount = boundedCount(buffer.readVarInt(), MAX_RARITY_WEIGHTS, "rarity weight");
            LinkedHashMap<ResourceLocation, Double> rarityWeights = new LinkedHashMap<>();
            for (int weightIndex = 0; weightIndex < weightCount; weightIndex++) {
                rarityWeights.put(buffer.readResourceLocation(), buffer.readDouble());
            }
            pools.add(new EditorBootstrap.PoolSummary(
                    id, name, enabled, choices, refreshes, maxOwned, excludeOwned, rarityWeights,
                    buffer.readUtf(MAX_TEXT), buffer.readVarInt()
            ));
        }

        int counterCount = boundedCount(buffer.readVarInt(), MAX_COUNTERS, "counter");
        ArrayList<EditorBootstrap.CounterSummary> counters = new ArrayList<>(counterCount);
        for (int index = 0; index < counterCount; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            String name = buffer.readUtf(MAX_TEXT);
            String event = buffer.readUtf(32);
            boolean enabled = buffer.readBoolean();
            boolean available = buffer.readBoolean();
            int categoryCount = boundedCount(
                    buffer.readVarInt(), MAX_CATEGORIES_PER_COUNTER, "counter category"
            );
            ArrayList<ResourceLocation> categories = new ArrayList<>(categoryCount);
            for (int categoryIndex = 0; categoryIndex < categoryCount; categoryIndex++) {
                categories.add(buffer.readResourceLocation());
            }
            counters.add(new EditorBootstrap.CounterSummary(
                    id, name, event, enabled, available, categories, buffer.readUtf(32),
                    buffer.readUtf(MAX_TEXT), buffer.readVarInt()
            ));
        }

        int layoutCount = boundedCount(buffer.readVarInt(), MAX_LAYOUTS, "layout");
        ArrayList<EditorBootstrap.LayoutSummary> layouts = new ArrayList<>(layoutCount);
        for (int index = 0; index < layoutCount; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            boolean enabled = buffer.readBoolean();
            boolean active = buffer.readBoolean();
            int priority = boundedValue(buffer.readVarInt(), -100_000, 100_000, "layout priority");
            java.util.Optional<ResourceLocation> background = buffer.readBoolean()
                    ? java.util.Optional.of(buffer.readResourceLocation()) : java.util.Optional.empty();
            int panelPercent = boundedValue(buffer.readVarInt(), 25, 70, "layout panel percent");
            int columns = boundedValue(buffer.readVarInt(), 1, 8, "layout columns");
            int rows = boundedValue(buffer.readVarInt(), 1, 10, "layout rows");
            int slotWidth = boundedValue(buffer.readVarInt(), 24, 128, "layout slot width");
            int slotHeight = boundedValue(buffer.readVarInt(), 22, 96, "layout slot height");
            int iconSize = boundedValue(buffer.readVarInt(), 8, 32, "layout icon size");
            int freeSlotCount = boundedValue(buffer.readVarInt(), 0, 256, "layout free slot count");
            int pageCount = boundedValue(buffer.readVarInt(), 0, 64, "layout page count");
            if (iconSize > slotHeight - 4) throw new IllegalArgumentException("Layout icon exceeds slot height");
            layouts.add(new EditorBootstrap.LayoutSummary(
                    id, enabled, active, priority, background, panelPercent, columns, rows,
                    slotWidth, slotHeight, iconSize, freeSlotCount, pageCount,
                    buffer.readBoolean(), buffer.readUtf(MAX_TEXT),
                    buffer.readVarInt()
            ));
        }

        int referenceCount = boundedCount(buffer.readVarInt(), MAX_REFERENCES, "definition reference");
        ArrayList<DefinitionReference> references = new ArrayList<>(referenceCount);
        for (int index = 0; index < referenceCount; index++) {
            DefinitionKind targetKind = buffer.readEnum(DefinitionKind.class);
            ResourceLocation targetId = buffer.readResourceLocation();
            references.add(new DefinitionReference(
                    new DefinitionTarget(targetKind, targetId),
                    buffer.readEnum(DefinitionKind.class),
                    buffer.readResourceLocation(),
                    buffer.readUtf(MAX_USAGE),
                    buffer.readUtf(MAX_PATH)
            ));
        }
        boolean referencesTruncated = buffer.readBoolean();

        List<ResourceLocation> entityTypeTags = readIds(buffer, "entity type tag");
        List<ResourceLocation> itemTags = readIds(buffer, "item tag");
        List<ResourceLocation> damageTypeTags = readIds(buffer, "damage type tag");

        int attributeCount = boundedCount(buffer.readVarInt(), MAX_ATTRIBUTES, "attribute");
        ArrayList<AttributeDescriptor> attributes = new ArrayList<>(attributeCount);
        for (int index = 0; index < attributeCount; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            String description = buffer.readUtf(MAX_TEXT);
            double defaultValue = buffer.readDouble();
            Double minimum = null;
            Double maximum = null;
            if (buffer.readBoolean()) {
                minimum = buffer.readDouble();
                maximum = buffer.readDouble();
            }
            attributes.add(new AttributeDescriptor(
                    id, description, defaultValue, minimum, maximum,
                    buffer.readBoolean(), buffer.readBoolean()
            ));
        }
        int configuredItemCount = boundedCount(
                buffer.readVarInt(), MAX_CONFIGURED_ITEMS, "configured item"
        );
        ArrayList<EditorBootstrap.ConfiguredItemSummary> configuredItems = new ArrayList<>(configuredItemCount);
        for (int index = 0; index < configuredItemCount; index++) {
            configuredItems.add(new EditorBootstrap.ConfiguredItemSummary(
                    buffer.readResourceLocation(), buffer.readUtf(MAX_TEXT),
                    buffer.readBoolean(), buffer.readBoolean(),
                    boundedValue(buffer.readVarInt(), 0, 16_777_215, "custom model data"),
                    buffer.readBoolean(), buffer.readUtf(MAX_TEXT), buffer.readVarInt()
            ));
        }
        boolean openScreen = buffer.readBoolean();
        return new BootstrapPacket(new EditorBootstrap(
                revision, hash, rarities, talents, pools, counters, layouts, references, referencesTruncated,
                entityTypeTags, itemTags, damageTypeTags, attributes, configuredItems
        ), openScreen);
    }

    static void handle(BootstrapPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptBootstrap(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }

    private static int boundedCount(int count, int maximum, String type) {
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid " + type + " count: " + count);
        }
        return count;
    }

    private static int boundedValue(int value, int minimum, int maximum, String type) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Invalid " + type + ": " + value);
        }
        return value;
    }

    private static void writeIds(FriendlyByteBuf buffer, List<ResourceLocation> ids, String type) {
        if (ids.size() > MAX_TAGS) throw new IllegalArgumentException(type + " count exceeds " + MAX_TAGS);
        buffer.writeVarInt(ids.size());
        ids.forEach(buffer::writeResourceLocation);
    }

    private static List<ResourceLocation> readIds(FriendlyByteBuf buffer, String type) {
        int count = boundedCount(buffer.readVarInt(), MAX_TAGS, type);
        ArrayList<ResourceLocation> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) ids.add(buffer.readResourceLocation());
        return List.copyOf(ids);
    }
}
