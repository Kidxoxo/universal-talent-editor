package com.kidxoxo.talenteditor.domain;

import java.util.List;

/** Immutable, server-authored UI. Coordinates use a uniformly fitted design canvas. */
public record UiDocument(int width, int height, int backdrop, List<Element> elements) {
    public UiDocument { elements = List.copyOf(elements); }
    public int slotCount() {
        return (int) elements.stream().filter(e -> e.type().equals("slot")).count();
    }
    public record Element(String id, String type, String anchor, int x, int y, int width, int height,
                          double opacity, int color, int hoverColor, int textColor,
                          String texture, String hoverTexture, String text, String binding,
                          String action, String target, int index, double textScale,
                          int iconSize, int iconX, int iconY, int textX, int textY, int rarityY, int bodyY,
                          String align, String buttonStyle, String label, boolean locked) {
        public Rect bounds(int canvasWidth, int canvasHeight) {
            int ax = anchor.endsWith("right") ? canvasWidth - width
                    : anchor.endsWith("center") ? (canvasWidth - width) / 2 : 0;
            int ay = anchor.startsWith("bottom") ? canvasHeight - height
                    : anchor.startsWith("middle") ? (canvasHeight - height) / 2 : 0;
            return new Rect(ax + x, ay + y, width, height);
        }
        public boolean interactive() { return type.equals("button") || type.equals("slot") || type.equals("offer"); }
    }
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double mx, double my) { return mx >= x && my >= y && mx < right() && my < bottom(); }
    }
    public record Viewport(double x, double y, double scale) {
        public static Viewport fit(UiDocument doc, double x, double y, double width, double height) {
            double scale = Math.max(0.001, Math.min(width / doc.width(), height / doc.height()));
            return new Viewport(x + (width - doc.width() * scale) / 2,
                    y + (height - doc.height() * scale) / 2, scale);
        }
        public double localX(double screenX) { return (screenX - x) / scale; }
        public double localY(double screenY) { return (screenY - y) / scale; }
    }
}
