package com.kidxoxo.talenteditor.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Forces the alpha channel of every vertex written through this consumer.
 *
 * <p>This is the batching-safe way to fade an entity: the value lands in the vertex data while it is
 * emitted, so it is unaffected by later global render-state changes (unlike
 * {@code RenderSystem#setShaderColor}, which is read at draw time, long after the entity was
 * submitted).</p>
 */
public final class AlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int alpha;

    public AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
        this.delegate = delegate;
        this.alpha = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        delegate.color(red, green, blue, this.alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex() {
        delegate.endVertex();
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        delegate.defaultColor(red, green, blue, this.alpha);
    }

    @Override
    public void unsetDefaultColor() {
        delegate.unsetDefaultColor();
    }
}
