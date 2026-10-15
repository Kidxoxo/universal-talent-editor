package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import com.kidxoxo.talenteditor.synthesis.SynthesisResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SynthesizeTalentPacket(ResourceLocation target) {
    static void encode(SynthesizeTalentPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.target());
    }

    static SynthesizeTalentPacket decode(FriendlyByteBuf buffer) {
        return new SynthesizeTalentPacket(buffer.readResourceLocation());
    }

    static void handle(SynthesizeTalentPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            SynthesisResult result = TalentEditorMod.SYNTHESES.synthesize(player, packet.target());
            EditorNetwork.sendSynthesisResult(player, result);
        }
        context.setPacketHandled(true);
    }
}
