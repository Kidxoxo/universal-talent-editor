package com.kidxoxo.talenteditor.mixin.client;

import com.kidxoxo.talenteditor.client.AlphaMultiBufferSource;
import com.kidxoxo.talenteditor.client.ClientVisualState;
import com.kidxoxo.talenteditor.client.PlayerRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the talent editor's per-player model scale and alpha on the render thread.
 *
 * <p>Both effects used to live in {@code RenderPlayerEvent.Pre/Post}. That approach could not work:
 * the event fires around the whole {@code LivingEntityRenderer#render} call, so the entity vertices
 * are only submitted to the shared buffer source and are drawn much later (after the event handlers
 * already restored the global shader colour). Writing alpha into the vertex colour during emission
 * and letting a translucent render type own the blend state is independent of that timing.</p>
 *
 * <p>The scale is applied on the pose stack before vanilla's own transforms. Vanilla translates the
 * model by {@code -1.501} inside a {@code scale(-1, -1, 1)} flip, which places the model's feet at
 * the entity origin, so scaling the outer matrix keeps the feet planted.</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, index = 5)
    private MultiBufferSource talentEditor$wrapMultiBufferSource(MultiBufferSource bufferSource) {
        if (bufferSource == null || !ClientVisualState.hasAlphaOverrides()) return bufferSource;
        return new AlphaMultiBufferSource(bufferSource);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void talentEditor$pushPlayerRenderState(
            LivingEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
            CallbackInfo callback) {
        Player player = entity instanceof Player candidate ? candidate : null;
        boolean scaled = false;
        if (player != null) {
            float scale = (float) ClientVisualState.get(player.getUUID()).scale();
            if (scale != 1.0F) {
                poseStack.pushPose();
                poseStack.scale(scale, scale, scale);
                scaled = true;
            }
        }
        PlayerRenderContext.push(player, scaled);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void talentEditor$popPlayerRenderState(
            LivingEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
            CallbackInfo callback) {
        PlayerRenderContext.Frame frame = PlayerRenderContext.pop();
        if (frame != null && frame.scaled()) {
            poseStack.popPose();
        }
    }
}
