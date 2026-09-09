package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record AddEffectAction(
        ResourceLocation effect,
        int durationTicks,
        int amplifier,
        ActionTarget target,
        boolean ambient,
        boolean showParticles,
        boolean showIcon
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "add_effect");

    public AddEffectAction {
        effect = Objects.requireNonNull(effect, "effect");
        target = Objects.requireNonNull(target, "target");
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
