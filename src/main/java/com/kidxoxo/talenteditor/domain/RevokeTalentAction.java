package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record RevokeTalentAction(
        ResourceLocation talent,
        boolean retainEffects,
        boolean retainCategoryContribution,
        boolean retainTriggers
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "revoke_talent");
    public RevokeTalentAction { talent = Objects.requireNonNull(talent, "talent"); }
    @Override public ResourceLocation type() { return TYPE; }
}
