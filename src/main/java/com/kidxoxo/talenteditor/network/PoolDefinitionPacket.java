package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.editor.DefinitionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PoolDefinitionPacket(long revision, ResourceLocation id, String json) {
    static void encode(PoolDefinitionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.revision());
        buffer.writeResourceLocation(packet.id());
        buffer.writeUtf(packet.json(), DefinitionService.MAX_JSON_CHARACTERS);
    }

    static PoolDefinitionPacket decode(FriendlyByteBuf buffer) {
        return new PoolDefinitionPacket(
                buffer.readVarLong(), buffer.readResourceLocation(),
                buffer.readUtf(DefinitionService.MAX_JSON_CHARACTERS)
        );
    }

    static void handle(PoolDefinitionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptPoolDefinition(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
