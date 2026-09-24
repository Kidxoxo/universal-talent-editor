package com.kidxoxo.talenteditor.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;

/**
 * Wraps the buffer source used while rendering a living entity so that every buffer the entity (and
 * its layers) request gets a translucent render type plus a forced vertex alpha.
 *
 * <p>The entity that is currently being rendered is looked up lazily at buffer-request time, which
 * happens after the render mixin pushed the render frame.</p>
 */
public final class AlphaMultiBufferSource implements MultiBufferSource {
    private final MultiBufferSource delegate;

    public AlphaMultiBufferSource(MultiBufferSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        Player player = PlayerRenderContext.current();
        float alpha = player == null
                ? 1.0F
                : (float) ClientVisualState.get(player.getUUID()).alpha();
        if (alpha >= 0.999F) {
            return delegate.getBuffer(renderType);
        }
        return new AlphaVertexConsumer(
                delegate.getBuffer(TranslucentRenderTypes.translucentVariant(renderType)),
                alpha
        );
    }
}
