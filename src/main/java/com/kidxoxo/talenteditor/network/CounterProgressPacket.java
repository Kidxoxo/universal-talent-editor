package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record CounterProgressPacket(List<Entry> entries) {
    public static final int MAX_ENTRIES = PlayerTalentStateFactory.MAX_COUNTERS;

    public CounterProgressPacket {
        entries = List.copyOf(entries);
    }

    static void encode(CounterProgressPacket packet, FriendlyByteBuf buffer) {
        TalentViewCodec.requireSize(packet.entries().size(), MAX_ENTRIES, "counter progress");
        buffer.writeVarInt(packet.entries().size());
        for (Entry entry : packet.entries()) {
            if (entry.value() < 0L
                    || entry.value() > com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE) {
                throw new IllegalArgumentException("Counter progress value exceeds bounds");
            }
            buffer.writeResourceLocation(entry.id());
            buffer.writeVarLong(entry.value());
        }
    }

    static CounterProgressPacket decode(FriendlyByteBuf buffer) {
        int count = TalentViewCodec.count(buffer.readVarInt(), MAX_ENTRIES, "counter progress");
        ArrayList<Entry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            long value = buffer.readVarLong();
            if (value < 0L || value > com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE) {
                throw new IllegalArgumentException("Invalid counter progress value");
            }
            entries.add(new Entry(id, value));
        }
        return new CounterProgressPacket(entries);
    }

    static void handle(CounterProgressPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptCounterProgress(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }

    public record Entry(ResourceLocation id, long value) {
    }
}
