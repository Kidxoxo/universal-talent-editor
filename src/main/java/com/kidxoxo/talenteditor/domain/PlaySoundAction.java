package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.util.Objects;

public record PlaySoundAction(
        ResourceLocation sound,
        SoundSource source,
        float volume,
        float pitch,
        int atTick
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "play_sound");

    public PlaySoundAction {
        sound = Objects.requireNonNull(sound, "sound");
        source = Objects.requireNonNull(source, "source");
    }

    public PlaySoundAction(ResourceLocation sound, SoundSource source, float volume, float pitch) {
        this(sound, source, volume, pitch, 0);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
