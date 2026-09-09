package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.MessageDisplay;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import com.kidxoxo.talenteditor.domain.ShowMessageAction;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.settings.EditorRuntimeSettings;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class TalentPresentationService {
    public void acquired(ServerPlayer player, TalentDefinition talent, boolean synthesized) {
        execute(player, talent.onAcquire(), synthesized);
        if (synthesized) execute(player, talent.onSynthesize(), true);
    }

    public static int execute(ServerPlayer player, List<CompiledAction> actions) {
        return execute(player, actions, false);
    }

    private static int execute(ServerPlayer player, List<CompiledAction> actions, boolean synthesis) {
        int applied = 0;
        for (CompiledAction action : actions) {
            if (action instanceof PlaySoundAction soundAction) {
                if (synthesis) playSynthesisSound(player, soundAction);
                else playVanillaSound(player, soundAction);
                applied++;
            } else if (action instanceof ShowMessageAction messageAction) {
                if (synthesis && TalentEditorMod.SETTINGS.synthesisMessageAudience()
                        == EditorRuntimeSettings.SynthesisMessageAudience.GLOBAL) {
                    player.server.getPlayerList().getPlayers().forEach(recipient ->
                            showMessage(recipient, messageAction));
                } else {
                    showMessage(player, messageAction);
                }
                applied++;
            } else {
                TalentEditorMod.LOGGER.warn("Ignored unsupported acquisition presentation action {}", action.type());
            }
        }
        return applied;
    }

    private static void playSynthesisSound(ServerPlayer player, PlaySoundAction action) {
        EditorRuntimeSettings settings = TalentEditorMod.SETTINGS.current();
        if (settings.synthesisSoundAudience() == EditorRuntimeSettings.SynthesisSoundAudience.GLOBAL) {
            player.server.getPlayerList().getPlayers().forEach(recipient ->
                    EditorNetwork.sendSynthesisSound(recipient, action, settings.soundOverlapPolicy()));
        } else {
            EditorNetwork.sendSynthesisSound(player, action, settings.soundOverlapPolicy());
        }
    }

    private static void playVanillaSound(ServerPlayer player, PlaySoundAction soundAction) {
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(soundAction.sound());
        Holder<SoundEvent> holder = sound == null
                ? Holder.direct(SoundEvent.createVariableRangeEvent(soundAction.sound()))
                : ForgeRegistries.SOUND_EVENTS.getHolder(soundAction.sound()).orElseThrow();
        player.connection.send(new ClientboundSoundPacket(
                holder, soundAction.source(), player.getX(), player.getY(), player.getZ(),
                soundAction.volume(), soundAction.pitch(), player.getRandom().nextLong()
        ));
    }

    private static void showMessage(ServerPlayer player, ShowMessageAction action) {
        var text = action.text().component();
        if (action.mode() == MessageDisplay.CHAT) {
            player.sendSystemMessage(text);
            return;
        }
        if (action.mode() == MessageDisplay.ACTION_BAR) {
            player.connection.send(new ClientboundSetActionBarTextPacket(text));
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                action.fadeInTicks(), action.stayTicks(), action.fadeOutTicks()
        ));
        action.subtitle().ifPresent(subtitle -> player.connection.send(
                new ClientboundSetSubtitleTextPacket(subtitle.component())
        ));
        player.connection.send(new ClientboundSetTitleTextPacket(text));
    }
}
