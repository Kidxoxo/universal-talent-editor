package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.draw.DrawResult;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record DrawActionPacket(Action action, UUID sessionId, ResourceLocation talentId) {
    private static final ResourceLocation NONE = new ResourceLocation(TalentEditorMod.MOD_ID, "none");

    public static DrawActionPacket refresh(UUID sessionId) {
        return new DrawActionPacket(Action.REFRESH, sessionId, NONE);
    }

    public static DrawActionPacket select(UUID sessionId, ResourceLocation talentId) {
        return new DrawActionPacket(Action.SELECT, sessionId, talentId);
    }

    static void encode(DrawActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action());
        buffer.writeUUID(packet.sessionId());
        buffer.writeResourceLocation(packet.talentId());
    }

    static DrawActionPacket decode(FriendlyByteBuf buffer) {
        return new DrawActionPacket(
                buffer.readEnum(Action.class), buffer.readUUID(), buffer.readResourceLocation()
        );
    }

    static void handle(DrawActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            int tokensBefore = PlayerTalents.drawTokens(player);
            DrawResult result = packet.action() == Action.REFRESH
                    ? TalentEditorMod.DRAWS.refresh(player, packet.sessionId())
                    : TalentEditorMod.DRAWS.select(player, packet.sessionId(), packet.talentId());
            if (result.changesPlayerState()) {
                TalentAttributeRuntime.refresh(player);
            }
            if (result.changesPlayerState() || tokensBefore != PlayerTalents.drawTokens(player)) {
                EditorNetwork.sendPlayerState(player, false);
            }
            EditorNetwork.sendDrawResult(player, result);
        }
        context.setPacketHandled(true);
    }

    public enum Action {
        REFRESH,
        SELECT
    }
}
