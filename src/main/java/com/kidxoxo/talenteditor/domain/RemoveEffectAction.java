package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record RemoveEffectAction(
        ResourceLocation effect,
        ActionTarget target
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "remove_effect");

    public RemoveEffectAction {
        effect = Objects.requireNonNull(effect, "effect");
        target = Objects.requireNonNull(target, "target");
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
