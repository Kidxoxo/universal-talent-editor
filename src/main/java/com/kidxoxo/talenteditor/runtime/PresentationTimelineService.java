package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import com.kidxoxo.talenteditor.domain.ShowMessageAction;
import com.kidxoxo.talenteditor.domain.SpawnParticlesAction;
import com.kidxoxo.talenteditor.domain.PresentationTransformAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;

/** Plays bounded acquisition and synthesis cues after their configured timeline offset. */
@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PresentationTimelineService {
    private static final ArrayList<Cue> PENDING = new ArrayList<>();

    private PresentationTimelineService() {}

    public static int atTick(CompiledAction action) {
        if (action instanceof PlaySoundAction sound) return sound.atTick();
        if (action instanceof ShowMessageAction message) return message.atTick();
        if (action instanceof SpawnParticlesAction particles) return particles.atTick();
        if (action instanceof PresentationTransformAction transform) return transform.atTick();
        return 0;
    }

    public static boolean schedule(ServerPlayer player, CompiledAction action, boolean synthesized) {
        if (PENDING.size() >= 2048 || PENDING.stream().filter(cue -> cue.player == player).count() >= 128) {
            return false;
        }
        PENDING.add(new Cue(player, action, synthesized, player.serverLevel().getGameTime() + atTick(action)));
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Iterator<Cue> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Cue cue = iterator.next();
            if (cue.player.isRemoved() || cue.player.server.getPlayerList().getPlayer(cue.player.getUUID()) != cue.player) {
                iterator.remove();
                continue;
            }
            if (cue.player.serverLevel().getGameTime() < cue.dueTick) continue;
            iterator.remove();
            if (cue.action instanceof SpawnParticlesAction particles) {
                ParticleAnimationService.start(cue.player, particles);
            } else if (cue.action instanceof PresentationTransformAction transform) {
                PresentationTransformService.start(cue.player, transform);
            } else {
                TalentPresentationService.executePresentation(cue.player, cue.action, cue.synthesized);
            }
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) { PENDING.clear(); }

    private record Cue(ServerPlayer player, CompiledAction action, boolean synthesized, long dueTick) {}
}
