package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record GrantTalentAction(
        ResourceLocation talent,
        boolean onlyIfNeverAcquired,
        boolean synthesized
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "grant_talent");
    public GrantTalentAction { talent = Objects.requireNonNull(talent, "talent"); }
    public GrantTalentAction(ResourceLocation talent, boolean onlyIfNeverAcquired) {
        this(talent, onlyIfNeverAcquired, false);
    }
    @Override public ResourceLocation type() { return TYPE; }
}
