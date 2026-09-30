package com.kidxoxo.talenteditor.domain;

/** RGB tint helpers shared by the editor, server packet creation, and client preview. */
public final class ParticleTint {
    private ParticleTint() {}

    public static int parse(String value) {
        if (value == null || value.isBlank()) return -1;
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Expected #RRGGBB");
        return Integer.parseInt(value.substring(1), 16);
    }

    public static int at(int from, int to, double progress) {
        if (from < 0 && to < 0) return -1;
        if (from < 0) from = to;
        if (to < 0) to = from;
        double t = Math.max(0, Math.min(1, progress));
        int red = channel(from >> 16, to >> 16, t);
        int green = channel(from >> 8, to >> 8, t);
        int blue = channel(from, to, t);
        return red << 16 | green << 8 | blue;
    }

    private static int channel(int from, int to, double progress) {
        return Math.max(0, Math.min(255, (int) Math.round((from & 255) * (1 - progress) + (to & 255) * progress)));
    }

    public static int rotateHue(int rgb, float degrees) {
        if (degrees == 0) return rgb & 0xFFFFFF;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        if (delta < 0.0001f) return rgb & 0xFFFFFF;
        float hue = max == r ? ((g - b) / delta) % 6
                : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4;
        hue = (hue * 60 + degrees) % 360;
        if (hue < 0) hue += 360;
        float saturation = max == 0 ? 0 : delta / max;
        float chroma = max * saturation;
        float secondary = chroma * (1 - Math.abs((hue / 60) % 2 - 1));
        float base = max - chroma;
        float rr, gg, bb;
        if (hue < 60) { rr = chroma; gg = secondary; bb = 0; }
        else if (hue < 120) { rr = secondary; gg = chroma; bb = 0; }
        else if (hue < 180) { rr = 0; gg = chroma; bb = secondary; }
        else if (hue < 240) { rr = 0; gg = secondary; bb = chroma; }
        else if (hue < 300) { rr = secondary; gg = 0; bb = chroma; }
        else { rr = chroma; gg = 0; bb = secondary; }
        return (Math.round((rr + base) * 255) << 16)
                | (Math.round((gg + base) * 255) << 8) | Math.round((bb + base) * 255);
    }
}
