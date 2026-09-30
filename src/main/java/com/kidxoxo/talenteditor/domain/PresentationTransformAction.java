package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

/** A short acquisition/synthesis scene that animates the player's model and position. */
public record PresentationTransformAction(int atTick, int durationTicks, double alphaFrom, double alphaTo,
                                          double scaleFrom, double scaleTo,
                                          double offsetX, double offsetY, double offsetZ, String positionMode)
        implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "presentation_transform");

    @Override public ResourceLocation type() { return TYPE; }

    /** Return moves out and back; hold reaches the target in the first quarter and stays there. */
    public double positionFactor(double progress) { return positionFactor(positionMode, progress); }

    public static double positionFactor(String mode, double progress) {
        double t = Math.max(0, Math.min(1, progress));
        if ("hold".equals(mode)) {
            double arrival = Math.min(1, t * 4);
            return arrival * arrival * (3 - 2 * arrival);
        }
        return Math.sin(Math.PI * t);
    }
}
