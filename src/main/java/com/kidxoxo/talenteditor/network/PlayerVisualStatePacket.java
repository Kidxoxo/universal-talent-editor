package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record PlayerVisualStatePacket(UUID player, PlayerTalentState.RenderView render) {
    static void encode(PlayerVisualStatePacket value, FriendlyByteBuf buffer) {
        buffer.writeUUID(value.player);
        buffer.writeDouble(value.render.alpha());
        buffer.writeDouble(value.render.scale());
        buffer.writeBoolean(value.render.hideNameplate());
    }
    static PlayerVisualStatePacket decode(FriendlyByteBuf buffer) {
        return new PlayerVisualStatePacket(buffer.readUUID(), new PlayerTalentState.RenderView(
                buffer.readDouble(), buffer.readDouble(), buffer.readBoolean()
        ));
    }
    static void handle(PlayerVisualStatePacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientVisualState.accept(packet));
        context.get().setPacketHandled(true);
    }
}
