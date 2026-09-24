package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

/** Client-visible player render controls contributed while the talent is active. */
public record PlayerRenderAction(
        NumericExpression alpha,
        NumericExpression scale,
        boolean hideNameplate
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "player_render");

    public PlayerRenderAction {
        if (alpha == null) throw new IllegalArgumentException("alpha expression is required");
        if (scale == null) throw new IllegalArgumentException("scale expression is required");
    }

    @Override public ResourceLocation type() { return TYPE; }
}
