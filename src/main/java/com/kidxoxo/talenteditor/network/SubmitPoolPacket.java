package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.editor.DefinitionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SubmitPoolPacket(
        long baseRevision,
        ResourceLocation id,
        String json,
        boolean commit, long requestId
) {
    public SubmitPoolPacket(long revision, ResourceLocation id, String json, boolean commit) {
        this(revision,id,json,commit,0);
    }

    static void encode(SubmitPoolPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.baseRevision());
        buffer.writeResourceLocation(packet.id());
        buffer.writeUtf(packet.json(), DefinitionService.MAX_JSON_CHARACTERS);
        buffer.writeBoolean(packet.commit());
        buffer.writeVarLong(packet.requestId());
    }

    static SubmitPoolPacket decode(FriendlyByteBuf buffer) {
        return new SubmitPoolPacket(
                buffer.readVarLong(), buffer.readResourceLocation(),
                buffer.readUtf(DefinitionService.MAX_JSON_CHARACTERS), buffer.readBoolean(), buffer.readVarLong()
        );
    }

    static void handle(SubmitPoolPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DefinitionSubmissionHandler.handle(com.kidxoxo.talenteditor.domain.DefinitionKind.POOL,
                packet.baseRevision(), packet.id(), packet.json(), packet.commit(), packet.requestId(), contextSupplier);
    }
}
