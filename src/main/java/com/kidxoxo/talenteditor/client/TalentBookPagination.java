package com.kidxoxo.talenteditor.client;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition.PageDefinition;
import net.minecraft.resources.ResourceLocation;

final class TalentBookPagination {
    private TalentBookPagination() {
    }

    /** Indices always refer to the original view list, including synthesis target/cost pairs. */
    static Placement place(List<ResourceLocation> items, List<PageDefinition> configured, int fallbackCapacity) {
        requireInputs(items.size(), List.of(), fallbackCapacity);
        HashMap<ResourceLocation, Integer> indices = new HashMap<>();
        for (int index = 0; index < items.size(); index++) indices.putIfAbsent(items.get(index), index);
        HashSet<ResourceLocation> reserved = new HashSet<>();
        configured.forEach(page -> page.slots().forEach(slot -> slot.talent().ifPresent(reserved::add)));
        ArrayList<Integer> automatic = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            if (!reserved.contains(items.get(index))) automatic.add(index);
        }
        ArrayList<List<Integer>> pages = new ArrayList<>();
        HashSet<ResourceLocation> claimed = new HashSet<>();
        int cursor = 0;
        for (PageDefinition page : configured) {
            ArrayList<Integer> entries = new ArrayList<>();
            for (var slot : page.slots()) {
                if (slot.talent().isPresent()) {
                    ResourceLocation talent = slot.talent().orElseThrow();
                    entries.add(claimed.add(talent) ? indices.getOrDefault(talent, -1) : -1);
                } else {
                    entries.add(cursor < automatic.size() ? automatic.get(cursor++) : -1);
                }
            }
            pages.add(List.copyOf(entries));
        }
        while (cursor < automatic.size() || pages.isEmpty()) {
            ArrayList<Integer> entries = new ArrayList<>();
            for (int slot = 0; slot < fallbackCapacity; slot++) {
                entries.add(cursor < automatic.size() ? automatic.get(cursor++) : -1);
            }
            pages.add(List.copyOf(entries));
        }
        return new Placement(pages);
    }

    record Placement(List<List<Integer>> pages) {
        Placement {
            pages = pages.stream().map(List::copyOf).toList();
        }

        int indexAt(int page, int slot) {
            if (page < 0 || page >= pages.size() || slot < 0 || slot >= pages.get(page).size()) return -1;
            return pages.get(page).get(slot);
        }

        int firstIndex(int page) {
            return pages.get(page).stream().filter(index -> index >= 0).findFirst().orElse(-1);
        }

        int pageFor(int itemIndex) {
            if (itemIndex < 0) return -1;
            for (int page = 0; page < pages.size(); page++) {
                if (pages.get(page).contains(itemIndex)) return page;
            }
            return -1;
        }
    }

    static int pageCount(int itemCount, List<Integer> configuredCapacities, int fallbackCapacity) {
        requireInputs(itemCount, configuredCapacities, fallbackCapacity);
        if (configuredCapacities.isEmpty()) {
            return Math.max(1, divideCeil(itemCount, fallbackCapacity));
        }
        int configuredCapacity = configuredCapacities.stream().mapToInt(Integer::intValue).sum();
        int remaining = Math.max(0, itemCount - configuredCapacity);
        return Math.max(1, configuredCapacities.size() + divideCeil(remaining, fallbackCapacity));
    }

    static int pageStart(int pageIndex, List<Integer> configuredCapacities, int fallbackCapacity) {
        requireInputs(0, configuredCapacities, fallbackCapacity);
        if (pageIndex < 0) throw new IllegalArgumentException("pageIndex must not be negative");
        int configuredBefore = Math.min(pageIndex, configuredCapacities.size());
        int start = 0;
        for (int index = 0; index < configuredBefore; index++) start += configuredCapacities.get(index);
        if (pageIndex > configuredCapacities.size()) {
            start += (pageIndex - configuredCapacities.size()) * fallbackCapacity;
        }
        return start;
    }

    static int pageForIndex(int itemIndex, List<Integer> configuredCapacities, int fallbackCapacity) {
        requireInputs(0, configuredCapacities, fallbackCapacity);
        int normalizedIndex = Math.max(0, itemIndex);
        int start = 0;
        for (int pageIndex = 0; pageIndex < configuredCapacities.size(); pageIndex++) {
            int capacity = configuredCapacities.get(pageIndex);
            if (normalizedIndex < start + capacity) return pageIndex;
            start += capacity;
        }
        return configuredCapacities.size() + (normalizedIndex - start) / fallbackCapacity;
    }

    private static int divideCeil(int value, int divisor) {
        return value == 0 ? 0 : (value + divisor - 1) / divisor;
    }

    private static void requireInputs(
            int itemCount,
            List<Integer> configuredCapacities,
            int fallbackCapacity
    ) {
        if (itemCount < 0) throw new IllegalArgumentException("itemCount must not be negative");
        if (fallbackCapacity < 1 || configuredCapacities.stream().anyMatch(capacity -> capacity < 1)) {
            throw new IllegalArgumentException("Page capacities must be positive");
        }
    }
}
