package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Small, read-only catalog used to populate the creative tab without opening the editor. */
public record ConfiguredItemCatalogPacket(long revision, List<Entry> items) {
    private static final int MAX_ITEMS = 10_000;
    private static final int MAX_TEXT = 512;
    private static final int MAX_LORE_LINES = 32;

    public ConfiguredItemCatalogPacket {
        if (revision < 0 || items.size() > MAX_ITEMS) throw new IllegalArgumentException("Invalid item catalog");
        items = List.copyOf(items);
    }

    public record Entry(ResourceLocation id, String name, List<String> lore, int customModelData, boolean glint) {
        public Entry {
            if (id == null || name == null || name.length() > MAX_TEXT || lore == null
                    || lore.size() > MAX_LORE_LINES || lore.stream().anyMatch(line -> line.length() > MAX_TEXT)
                    || customModelData < 0 || customModelData > 16_777_215) {
                throw new IllegalArgumentException("Invalid creative item entry");
            }
            lore = List.copyOf(lore);
        }
    }

    static void encode(ConfiguredItemCatalogPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.revision());
        buffer.writeVarInt(packet.items().size());
        for (Entry item : packet.items()) {
            buffer.writeResourceLocation(item.id());
            buffer.writeUtf(item.name(), MAX_TEXT);
            buffer.writeVarInt(item.lore().size());
            item.lore().forEach(line -> buffer.writeUtf(line, MAX_TEXT));
            buffer.writeVarInt(item.customModelData());
            buffer.writeBoolean(item.glint());
        }
    }

    static ConfiguredItemCatalogPacket decode(FriendlyByteBuf buffer) {
        long revision = buffer.readVarLong();
        int count = boundedCount(buffer.readVarInt(), MAX_ITEMS);
        ArrayList<Entry> items = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            String name = buffer.readUtf(MAX_TEXT);
            int loreCount = boundedCount(buffer.readVarInt(), MAX_LORE_LINES);
            ArrayList<String> lore = new ArrayList<>(loreCount);
            for (int line = 0; line < loreCount; line++) lore.add(buffer.readUtf(MAX_TEXT));
            items.add(new Entry(id, name, lore, buffer.readVarInt(), buffer.readBoolean()));
        }
        return new ConfiguredItemCatalogPacket(revision, items);
    }

    static void handle(ConfiguredItemCatalogPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientCreativeItems.accept(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }

    private static int boundedCount(int value, int maximum) {
        if (value < 0 || value > maximum) throw new IllegalArgumentException("Invalid catalog count: " + value);
        return value;
    }
}
