package com.kidxoxo.talenteditor.client;

/** Client render-thread state used only while a particle preview is open. */
public final class ParticlePreviewCameraDistance {
    private static double activeDistance = Double.NaN;

    private ParticlePreviewCameraDistance() {}

    static void activate(double distance) { activeDistance = distance; }

    static void deactivate() { activeDistance = Double.NaN; }

    public static double orDefault(double vanillaDistance) {
        return Double.isNaN(activeDistance) ? vanillaDistance : activeDistance;
    }
}
