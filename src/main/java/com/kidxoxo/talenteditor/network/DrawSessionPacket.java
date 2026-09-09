package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.function.Supplier;

public record DrawSessionPacket(DrawSessionView view) {
    private static final int MAX_OFFERS = 5;
    private static final int MAX_POOL_NAME = 1_024;
    private static final int MAX_RARITY_PROBABILITIES = 256;

    static void encode(DrawSessionPacket packet, FriendlyByteBuf buffer) {
        DrawSessionView view = packet.view();
        TalentViewCodec.requireSize(view.offers().size(), MAX_OFFERS, "draw offer");
        buffer.writeUUID(view.sessionId());
        buffer.writeVarLong(view.definitionRevision());
        buffer.writeResourceLocation(view.poolId());
        buffer.writeUtf(view.poolName(), MAX_POOL_NAME);
        buffer.writeVarInt(view.drawTokens());
        buffer.writeVarInt(view.refreshesRemaining());
        buffer.writeVarInt(view.iconSize());
        buffer.writeDouble(view.luckValue());
        buffer.writeVarInt(view.pityProgress());
        buffer.writeVarInt(view.pityAfter());
        buffer.writeBoolean(view.pityActive());
        TalentViewCodec.requireSize(
                view.rarityProbabilities().size(), MAX_RARITY_PROBABILITIES, "rarity probability"
        );
        buffer.writeVarInt(view.rarityProbabilities().size());
        view.rarityProbabilities().forEach((rarity, probability) -> {
            buffer.writeResourceLocation(rarity);
            buffer.writeDouble(probability);
        });
        buffer.writeVarInt(view.offers().size());
        view.offers().forEach(talent -> TalentViewCodec.encode(buffer, talent));
    }

    static DrawSessionPacket decode(FriendlyByteBuf buffer) {
        var sessionId = buffer.readUUID();
        long revision = buffer.readVarLong();
        var poolId = buffer.readResourceLocation();
        String poolName = buffer.readUtf(MAX_POOL_NAME);
        int tokens = buffer.readVarInt();
        int refreshes = buffer.readVarInt();
        int iconSize = buffer.readVarInt();
        double luck = buffer.readDouble();
        int pityProgress = buffer.readVarInt();
        int pityAfter = buffer.readVarInt();
        boolean pityActive = buffer.readBoolean();
        if (tokens < 0 || refreshes < 0 || iconSize < 8 || iconSize > 64
                || pityProgress < 0 || pityAfter < 0 || !Double.isFinite(luck)) {
            throw new IllegalArgumentException("Invalid draw session values");
        }
        int probabilityCount = TalentViewCodec.count(
                buffer.readVarInt(), MAX_RARITY_PROBABILITIES, "rarity probability"
        );
        java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation, Double> probabilities =
                new java.util.LinkedHashMap<>();
        for (int index = 0; index < probabilityCount; index++) {
            var rarity = buffer.readResourceLocation();
            double probability = buffer.readDouble();
            if (!Double.isFinite(probability) || probability < 0.0D || probability > 1.0D) {
                throw new IllegalArgumentException("Invalid rarity probability");
            }
            probabilities.put(rarity, probability);
        }
        int count = TalentViewCodec.count(buffer.readVarInt(), MAX_OFFERS, "draw offer");
        ArrayList<PlayerTalentState.TalentView> offers = new ArrayList<>(count);
        for (int index = 0; index < count; index++) offers.add(TalentViewCodec.decode(buffer));
        return new DrawSessionPacket(new DrawSessionView(
                sessionId, revision, poolId, poolName, tokens, refreshes, iconSize,
                luck, pityProgress, pityAfter, pityActive, probabilities, offers
        ));
    }

    static void handle(DrawSessionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptDrawSession(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
