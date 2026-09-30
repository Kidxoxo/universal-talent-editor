package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

/** A bounded particle animation centered on the player. Only simple registered particle types are accepted. */
public record SpawnParticlesAction(ResourceLocation particle, String anchor, String motion,
                                   int count, double radius, double speed, int durationTicks,
                                   int intervalTicks, double viewDistance, int cooldownTicks,
                                   int atTick, double alphaFrom, double alphaTo,
                                   int colorFrom, int colorTo, float hueShiftDegrees,
                                   double sizeFrom, double sizeTo)
        implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "spawn_particles");

    public SpawnParticlesAction(ResourceLocation particle, String anchor, String motion,
                                int count, double radius, double speed, int durationTicks,
                                int intervalTicks, double viewDistance, int cooldownTicks) {
        this(particle, anchor, motion, count, radius, speed, durationTicks, intervalTicks,
                viewDistance, cooldownTicks, 0, 1, 1, -1, -1, 0, 1, 1);
    }

    @Override public ResourceLocation type() { return TYPE; }
}
