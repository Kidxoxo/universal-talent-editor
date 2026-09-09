package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.editor.DefinitionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TalentDefinitionPacket(long revision, ResourceLocation id, String json) {
    static void encode(TalentDefinitionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.revision());
        buffer.writeResourceLocation(packet.id());
        buffer.writeUtf(packet.json(), DefinitionService.MAX_JSON_CHARACTERS);
    }

    static TalentDefinitionPacket decode(FriendlyByteBuf buffer) {
        return new TalentDefinitionPacket(
                buffer.readVarLong(),
                buffer.readResourceLocation(),
                buffer.readUtf(DefinitionService.MAX_JSON_CHARACTERS)
        );
    }

    static void handle(TalentDefinitionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptTalentDefinition(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
