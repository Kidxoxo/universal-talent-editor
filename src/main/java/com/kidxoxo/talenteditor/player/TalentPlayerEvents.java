package com.kidxoxo.talenteditor.player;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.domain.CounterResetTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TalentPlayerEvents {
    private static final ResourceLocation CAPABILITY_ID =
            new ResourceLocation(TalentEditorMod.MOD_ID, "player_talents");

    private TalentPlayerEvents() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        PlayerTalentProvider provider = new PlayerTalentProvider();
        event.addCapability(CAPABILITY_ID, provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        event.getOriginal().reviveCaps();
        try {
            PlayerTalentData original = event.getOriginal()
                    .getCapability(TalentCapabilities.PLAYER_TALENTS).resolve().orElse(null);
            PlayerTalentData copy = player.getCapability(TalentCapabilities.PLAYER_TALENTS)
                    .resolve().orElse(null);
            if (original == null || copy == null) {
                TalentEditorMod.LOGGER.error(
                        "Player talent capability unavailable during clone for {} (death={})",
                        player.getGameProfile().getName(), event.isWasDeath()
                );
                return;
            }
            copy.copyFrom(original);
            if (event.isWasDeath()) {
                TalentEditorMod.COUNTERS.resetForTrigger(player, CounterResetTrigger.DEATH);
            }
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentEditorMod.COUNTERS.resetForTrigger(player, CounterResetTrigger.LOGIN);
            refreshAndSync(player);
            EditorNetwork.sendResourceRequirements(player);
        }
    }

    @SubscribeEvent
    public static void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refreshAndSync(player);
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refreshAndSync(player);
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 10 == 0) TalentAttributeRuntime.refreshDynamic(player);
        if (player.tickCount % 20 == 0 && TalentEditorMod.SETTINGS.automaticSynthesis()
                && TalentEditorMod.SYNTHESES.synthesizeReady(player, 64) > 0) {
            TalentAttributeRuntime.refresh(player);
            EditorNetwork.sendPlayerState(player, false);
        }
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentAttributeRuntime.removeTracked(player);
            TalentEditorMod.DRAWS.removeSession(player);
            TalentEditorMod.COUNTERS.removePlayer(player);
            TalentEditorMod.CLIENT_RESOURCES.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void dataPackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            refreshAndSync(event.getPlayer());
            EditorNetwork.sendResourceRequirements(event.getPlayer());
        } else {
            event.getPlayerList().getPlayers().forEach(player -> {
                refreshAndSync(player);
                EditorNetwork.sendResourceRequirements(player);
            });
        }
    }

    private static void refreshAndSync(ServerPlayer player) {
        TalentAttributeRuntime.refresh(player);
        EditorNetwork.sendPlayerState(player, false);
    }
}
