package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.editor.DefinitionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ConfiguredItemDefinitionPacket(long revision, ResourceLocation id, String json) {
    static void encode(ConfiguredItemDefinitionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.revision());
        buffer.writeResourceLocation(packet.id());
        buffer.writeUtf(packet.json(), DefinitionService.MAX_JSON_CHARACTERS);
    }

    static ConfiguredItemDefinitionPacket decode(FriendlyByteBuf buffer) {
        return new ConfiguredItemDefinitionPacket(
                buffer.readVarLong(), buffer.readResourceLocation(),
                buffer.readUtf(DefinitionService.MAX_JSON_CHARACTERS)
        );
    }

    static void handle(ConfiguredItemDefinitionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptConfiguredItemDefinition(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
