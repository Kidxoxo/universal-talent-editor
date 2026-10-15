package com.kidxoxo.talenteditor.client;

import java.util.*;

/** Keep content-only edits valid for the legacy renderer and strict layout compiler. */
final class LayoutContentSlots {
    private LayoutContentSlots() {}

    static List<LayoutDraft.SlotDraft> template(List<LayoutDraft.SlotDraft> source, int capacity) {
        if (capacity < 1 || capacity > 256) throw new IllegalArgumentException("Page capacity must be 1–256");
        ArrayList<LayoutDraft.SlotDraft> result = new ArrayList<>(source.subList(0, Math.min(source.size(), capacity)));
        while (result.size() < capacity) {
            var next = next(result);
            if (next.isEmpty()) {
                // This is a new-page template, never an existing page with saved bindings.
                result.clear();
            } else result.add(next.orElseThrow());
        }
        return List.copyOf(result);
    }

    static Optional<LayoutDraft.SlotDraft> next(List<LayoutDraft.SlotDraft> slots) {
        if (slots.size() >= 256) return Optional.empty();
        Set<String> ids = new HashSet<>();
        slots.forEach(slot -> ids.add(slot.id()));
        int suffix = 0;
        while (ids.contains("slot_" + suffix)) suffix++;
        for (int y = 0; y + 30 <= 2048; y += 32) {
            for (int x = 0; x + 60 <= 2048; x += 64) {
                var candidate = new LayoutDraft.SlotDraft("slot_" + suffix, x, y, 60, 30, 16);
                if (slots.stream().noneMatch(slot -> overlaps(slot, candidate))) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static boolean overlaps(LayoutDraft.SlotDraft a, LayoutDraft.SlotDraft b) {
        return a.x() < b.x() + b.width() && b.x() < a.x() + a.width()
                && a.y() < b.y() + b.height() && b.y() < a.y() + a.height();
    }
}
