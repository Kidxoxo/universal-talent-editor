package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Declares a client HUD bar backed by a persistent numeric state. */
public record HudResourceAction(
        ResourceLocation state,
        NumericExpression maximum,
        int color,
        int order,
        boolean showWhenFull
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "hud_resource");

    public HudResourceAction {
        state = Objects.requireNonNull(state, "state");
        maximum = Objects.requireNonNull(maximum, "maximum");
        color &= 0xFFFFFF;
        if (order < -10_000 || order > 10_000) throw new IllegalArgumentException("Invalid HUD order");
    }

    @Override public ResourceLocation type() { return TYPE; }
}
