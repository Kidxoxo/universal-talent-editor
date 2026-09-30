package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.PresentationTransformAction;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.PresentationTransformPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Drives short player presentation cues. Movement is server authoritative and collision checked. */
@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PresentationTransformService {
    private static final Map<UUID, Animation> ACTIVE = new HashMap<>();

    private PresentationTransformService() {}

    public static boolean start(ServerPlayer player, PresentationTransformAction action) {
        if (ACTIVE.containsKey(player.getUUID()) || ACTIVE.size() >= 128) return false;
        boolean moving = action.offsetX() != 0 || action.offsetY() != 0 || action.offsetZ() != 0;
        Animation animation = new Animation(player, action, player.serverLevel().getGameTime(),
                player.isNoGravity(), moving);
        ACTIVE.put(player.getUUID(), animation);
        if (moving) player.setNoGravity(true);
        update(animation, 0);
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Animation> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            Animation animation = iterator.next();
            ServerPlayer player = animation.player;
            if (player.isRemoved() || player.server.getPlayerList().getPlayer(player.getUUID()) != player) {
                finish(animation);
                iterator.remove();
                continue;
            }
            long age = player.serverLevel().getGameTime() - animation.started;
            if (age < 0 || age >= animation.action.durationTicks()) {
                finish(animation);
                iterator.remove();
            } else if (age > 0) update(animation, (int) age);
        }
    }

    private static void update(Animation animation, int age) {
        ServerPlayer player = animation.player;
        PresentationTransformAction action = animation.action;
        double progress = (double) age / (action.durationTicks() - 1);
        double factor = action.positionFactor(progress);
        if (animation.moving && !animation.blocked) {
            double delta = factor - animation.lastFactor;
            double x = action.offsetX() * delta, y = action.offsetY() * delta, z = action.offsetZ() * delta;
            if (player.serverLevel().noCollision(player, player.getBoundingBox().move(x, y, z))) {
                player.teleportTo(player.getX() + x, player.getY() + y, player.getZ() + z);
                player.setDeltaMovement(0, 0, 0);
                player.fallDistance = 0;
                animation.lastFactor = factor;
            } else animation.blocked = true;
        }
        float alpha = (float) (action.alphaFrom() + (action.alphaTo() - action.alphaFrom()) * progress);
        float scale = (float) (action.scaleFrom() + (action.scaleTo() - action.scaleFrom()) * progress);
        EditorNetwork.sendPresentationTransform(player,
                new PresentationTransformPacket(player.getUUID(), alpha, scale, 0, 0, 0, false));
    }

    private static void finish(Animation animation) {
        if (animation.moving) animation.player.setNoGravity(animation.originalNoGravity);
        if (!animation.player.isRemoved() && animation.player.server.getPlayerList()
                .getPlayer(animation.player.getUUID()) == animation.player)
            EditorNetwork.sendPresentationTransform(animation.player,
                    new PresentationTransformPacket(animation.player.getUUID(), 1, 1, 0, 0, 0, true));
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        for (Animation animation : ACTIVE.values()) finish(animation);
        ACTIVE.clear();
    }

    private static final class Animation {
        final ServerPlayer player;
        final PresentationTransformAction action;
        final long started;
        final boolean originalNoGravity;
        final boolean moving;
        boolean blocked;
        double lastFactor;

        Animation(ServerPlayer player, PresentationTransformAction action, long started,
                  boolean originalNoGravity, boolean moving) {
            this.player = player;
            this.action = action;
            this.started = started;
            this.originalNoGravity = originalNoGravity;
            this.moving = moving;
        }
    }
}
