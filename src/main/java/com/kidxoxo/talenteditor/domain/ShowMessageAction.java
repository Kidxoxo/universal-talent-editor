package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

public record ShowMessageAction(
        MessageDisplay mode,
        LocalizedText text,
        Optional<LocalizedText> subtitle,
        int fadeInTicks,
        int stayTicks,
        int fadeOutTicks
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "show_message");

    public ShowMessageAction {
        mode = Objects.requireNonNull(mode, "mode");
        text = Objects.requireNonNull(text, "text");
        subtitle = subtitle == null ? Optional.empty() : subtitle;
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
