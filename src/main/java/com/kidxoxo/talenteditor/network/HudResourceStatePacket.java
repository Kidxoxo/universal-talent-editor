package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Lightweight HUD update used when a persistent numeric state changes frequently. */
public record HudResourceStatePacket(List<PlayerTalentState.HudResourceView> resources) {
    public static final int MAX_RESOURCES = 32;

    public HudResourceStatePacket {
        resources = List.copyOf(resources);
        if (resources.size() > MAX_RESOURCES) throw new IllegalArgumentException("Too many HUD resources");
    }

    static void encode(HudResourceStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.resources().size());
        for (PlayerTalentState.HudResourceView resource : packet.resources()) {
            buffer.writeResourceLocation(resource.state());
            TalentViewCodec.encodeText(buffer, resource.name());
            buffer.writeDouble(resource.value());
            buffer.writeDouble(resource.maximum());
            buffer.writeVarInt(resource.color());
            buffer.writeVarInt(resource.order());
            buffer.writeBoolean(resource.showWhenFull());
        }
    }

    static HudResourceStatePacket decode(FriendlyByteBuf buffer) {
        int count = TalentViewCodec.count(buffer.readVarInt(), MAX_RESOURCES, "HUD resource");
        ArrayList<PlayerTalentState.HudResourceView> resources = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            resources.add(new PlayerTalentState.HudResourceView(
                    buffer.readResourceLocation(), TalentViewCodec.decodeText(buffer),
                    buffer.readDouble(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()
            ));
        }
        return new HudResourceStatePacket(resources);
    }

    static void handle(HudResourceStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptHudResourceState(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
