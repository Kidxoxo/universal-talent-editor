package com.kidxoxo.talenteditor.mixin.client;

import com.kidxoxo.talenteditor.client.ParticlePreviewCameraDistance;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Replaces vanilla's four-block third-person distance during particle previews. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyArg(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"))
    private double talentEditor$previewDistance(double vanillaDistance) {
        return ParticlePreviewCameraDistance.orDefault(vanillaDistance);
    }
}
