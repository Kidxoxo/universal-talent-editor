package com.kidxoxo.talenteditor.player;

import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.Map;
import java.util.Optional;

public final class PlayerTalents {
    private PlayerTalents() {
    }

    public static Set<ResourceLocation> owned(ServerPlayer player) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(PlayerTalentData::owned)
                .orElse(Set.of());
    }

    public static boolean grant(ServerPlayer player, ResourceLocation talent) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.grant(talent))
                .orElse(false);
    }

    public static boolean revoke(ServerPlayer player, ResourceLocation talent) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.revoke(talent))
                .orElse(false);
    }

    public static Map<ResourceLocation, Long> counters(ServerPlayer player) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(PlayerTalentData::counters)
                .orElse(Map.of());
    }

    public static long counter(ServerPlayer player, ResourceLocation id) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.counter(id))
                .orElse(0L);
    }

    public static long setCounter(ServerPlayer player, ResourceLocation id, long value) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.setCounter(id, value))
                .orElse(0L);
    }

    public static long addCounter(ServerPlayer player, ResourceLocation id, long amount) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.addCounter(id, amount))
                .orElse(0L);
    }

    public static long advanceCounterLifecycleTick(ServerPlayer player) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(PlayerTalentData::advanceCounterLifecycleTick)
                .orElse(0L);
    }

    public static long counterLifecycleTicks(ServerPlayer player) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(PlayerTalentData::counterLifecycleTicks)
                .orElse(0L);
    }

    public static long triggerLastActivation(ServerPlayer player, String id) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.triggerLastActivation(id))
                .orElse(-1L);
    }

    public static boolean setTriggerLastActivation(ServerPlayer player, String id, long tick) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.setTriggerLastActivation(id, tick))
                .orElse(false);
    }

    public static int triggerActivationCount(ServerPlayer player, String id) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.triggerActivationCount(id))
                .orElse(0);
    }

    public static boolean canRecordTriggerActivation(ServerPlayer player, String id) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.canRecordTriggerActivation(id))
                .orElse(false);
    }

    public static boolean incrementTriggerActivation(ServerPlayer player, String id) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.incrementTriggerActivation(id))
                .orElse(false);
    }

    public static boolean claimTriggerDistanceInterval(
            ServerPlayer player, String id, double distance, double interval
    ) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.claimTriggerDistanceInterval(id, distance, interval))
                .orElse(false);
    }

    public static int drawTokens(ServerPlayer player) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(PlayerTalentData::drawTokens)
                .orElse(0);
    }

    public static int addDrawTokens(ServerPlayer player, int amount) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.addDrawTokens(amount))
                .orElse(0);
    }

    public static int setDrawTokens(ServerPlayer player, int amount) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> {
                    data.setDrawTokens(amount);
                    return data.drawTokens();
                })
                .orElse(0);
    }

    public static boolean consumeDrawToken(ServerPlayer player) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(PlayerTalentData::consumeDrawToken)
                .orElse(false);
    }

    public static boolean acquireWithDrawToken(ServerPlayer player, ResourceLocation talent) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.acquireWithDrawToken(talent))
                .orElse(false);
    }

    public static int drawPity(ServerPlayer player, ResourceLocation pool) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.drawPity(pool))
                .orElse(0);
    }

    public static int recordDrawPity(ServerPlayer player, ResourceLocation pool, boolean satisfied, int maximum) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.recordDrawPity(pool, satisfied, maximum))
                .orElse(0);
    }

    public static boolean exchangeDrawTokenForCounter(ServerPlayer player, ResourceLocation counter, long amount) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.exchangeDrawTokenForCounter(counter, amount))
                .orElse(false);
    }

    public static boolean queueDrawConstraint(ServerPlayer player, QueueDrawConstraintAction constraint) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.queueDrawConstraint(constraint))
                .orElse(false);
    }

    public static Optional<QueueDrawConstraintAction> nextDrawConstraint(
            ServerPlayer player,
            ResourceLocation pool
    ) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .resolve()
                .flatMap(data -> data.nextDrawConstraint(pool));
    }

    public static boolean consumeNextDrawConstraint(ServerPlayer player, ResourceLocation pool) {
        return player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                .map(data -> data.consumeNextDrawConstraint(pool))
                .orElse(false);
    }
}
