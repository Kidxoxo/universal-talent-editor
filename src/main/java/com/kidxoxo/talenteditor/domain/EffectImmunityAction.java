package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record EffectImmunityAction(ResourceLocation effect) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "effect_immunity");

    public EffectImmunityAction {
        effect = Objects.requireNonNull(effect, "effect");
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
