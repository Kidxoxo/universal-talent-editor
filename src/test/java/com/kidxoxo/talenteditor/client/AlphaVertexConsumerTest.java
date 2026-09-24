package com.kidxoxo.talenteditor.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlphaVertexConsumerTest {

    @Test
    void replacesAlphaAndKeepsTheOtherChannels() {
        Recording recording = new Recording();
        AlphaVertexConsumer consumer = new AlphaVertexConsumer(recording, 0.5F);

        consumer.color(10, 20, 30, 255);

        assertEquals(10, recording.red);
        assertEquals(20, recording.green);
        assertEquals(30, recording.blue);
        assertEquals(128, recording.alpha);
    }

    @Test
    void defaultColorPathIsAlsoForced() {
        Recording recording = new Recording();
        AlphaVertexConsumer consumer = new AlphaVertexConsumer(recording, 0.25F);

        consumer.defaultColor(1, 2, 3, 255);

        assertEquals(64, recording.defaultAlpha);
    }

    @Test
    void floatColorHelperStillRoutesThroughTheForcedAlpha() {
        Recording recording = new Recording();
        AlphaVertexConsumer consumer = new AlphaVertexConsumer(recording, 1.0F);

        consumer.color(1.0F, 1.0F, 1.0F, 0.0F);

        assertEquals(255, recording.alpha);
    }

    @Test
    void verticesAreForwarded() {
        Recording recording = new Recording();
        AlphaVertexConsumer consumer = new AlphaVertexConsumer(recording, 0.5F);

        consumer.vertex(1.0D, 2.0D, 3.0D).endVertex();

        assertEquals(1, recording.vertices);
        assertEquals(1, recording.endVertices);
    }

    private static final class Recording implements VertexConsumer {
        private int red;
        private int green;
        private int blue;
        private int alpha;
        private int defaultAlpha = -1;
        private int vertices;
        private int endVertices;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            vertices++;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
            endVertices++;
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            defaultAlpha = alpha;
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
