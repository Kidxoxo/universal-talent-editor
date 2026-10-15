package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.SynthesisSoundPacket;
import com.kidxoxo.talenteditor.settings.EditorRuntimeSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.util.RandomSource;

import java.util.ArrayDeque;

/** Owns synthesis audio and acquisition voice cues; ordinary Minecraft audio is untouched. */
public final class SynthesisSoundPlayback {
    private static final int MAX_QUEUED_SOUNDS = 64;
    private static final ArrayDeque<SynthesisSoundPacket> QUEUE = new ArrayDeque<>();
    private static SoundInstance current;

    private SynthesisSoundPlayback() {
    }

    public static void accept(SynthesisSoundPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.overlapPolicy() == EditorRuntimeSettings.SoundOverlapPolicy.IMMEDIATE) {
            QUEUE.clear();
            if (current != null) minecraft.getSoundManager().stop(current);
            current = null;
            play(minecraft, packet);
            return;
        }
        if (current == null || !minecraft.getSoundManager().isActive(current)) {
            current = null;
            play(minecraft, packet);
            return;
        }
        if (QUEUE.size() >= MAX_QUEUED_SOUNDS) QUEUE.removeFirst();
        QUEUE.addLast(packet);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null) {
            clear(minecraft);
            return;
        }
        if (current == null || minecraft.getSoundManager().isActive(current)) return;
        current = null;
        SynthesisSoundPacket next = QUEUE.pollFirst();
        if (next != null) play(minecraft, next);
    }

    private static void play(Minecraft minecraft, SynthesisSoundPacket packet) {
        current = new SimpleSoundInstance(
                packet.sound(), packet.source(), packet.volume(), packet.pitch(), RandomSource.create(),
                false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true
        );
        minecraft.getSoundManager().play(current);
    }

    private static void clear(Minecraft minecraft) {
        QUEUE.clear();
        if (current != null) minecraft.getSoundManager().stop(current);
        current = null;
    }
}
