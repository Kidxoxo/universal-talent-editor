package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.settings.EditorSettingsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EditorManagementResultPacket(
        EditorSettingsService.ManagementAction action,
        boolean successful,
        String detail
) {
    private static final int MAX_DETAIL = 4_096;

    public EditorManagementResultPacket {
        detail = detail == null ? "" : detail;
        if (detail.length() > MAX_DETAIL) detail = detail.substring(0, MAX_DETAIL);
    }

    static void encode(EditorManagementResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action());
        buffer.writeBoolean(packet.successful());
        buffer.writeUtf(packet.detail(), MAX_DETAIL);
    }

    static EditorManagementResultPacket decode(FriendlyByteBuf buffer) {
        return new EditorManagementResultPacket(
                buffer.readEnum(EditorSettingsService.ManagementAction.class),
                buffer.readBoolean(), buffer.readUtf(MAX_DETAIL)
        );
    }

    static void handle(EditorManagementResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptManagementResult(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
