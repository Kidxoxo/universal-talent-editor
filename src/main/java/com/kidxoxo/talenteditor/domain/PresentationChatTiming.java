package com.kidxoxo.talenteditor.domain;

/** Chat keeps vanilla's final one-second fade; only the full-opacity hold is extended. */
public final class PresentationChatTiming {
    private PresentationChatTiming() {}

    public static int visibleTicks(int stayTicks) {
        return Math.max(200, Math.min(12_000, stayTicks));
    }

    public static int timestampOffset(int stayTicks) {
        return visibleTicks(stayTicks) - 200;
    }

    public static double opacity(int elapsed, int stayTicks) {
        double factor = Math.max(0, Math.min(1, (visibleTicks(stayTicks) - elapsed) / 20.0));
        return factor * factor;
    }
}
