package com.kidxoxo.talenteditor.runtime;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TriggerTraceBuffer {
    public static final int DEFAULT_CAPACITY = 256;

    private final int capacity;
    private final ArrayDeque<TriggerTraceEntry> entries = new ArrayDeque<>();
    private volatile boolean enabled;
    private long nextSequence = 1L;
    private long overwritten;

    public TriggerTraceBuffer() {
        this(DEFAULT_CAPACITY);
    }

    TriggerTraceBuffer(int capacity) {
        if (capacity < 1 || capacity > 10_000) throw new IllegalArgumentException("Invalid trace capacity");
        this.capacity = capacity;
    }

    public boolean enabled() {
        return enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void record(
            long playerTick,
            String playerName,
            ResourceLocation event,
            TriggerBinding binding,
            TriggerTraceEntry.Outcome outcome,
            double effectiveChance,
            int appliedActions,
            int attemptedActions,
            List<TriggerTraceEntry.ActionResult> actionResults
    ) {
        if (!enabled) return;
        synchronized (this) {
            if (!enabled) return;
            if (entries.size() == capacity) {
                entries.removeFirst();
                overwritten++;
            }
            entries.addLast(new TriggerTraceEntry(
                    nextSequence++, playerTick, playerName, event, binding.talentId(), binding.trigger().id(),
                    outcome, effectiveChance, appliedActions, attemptedActions,
                    actionResults.stream().limit(TriggerTraceEntry.MAX_ACTION_RESULTS).toList()
            ));
        }
    }

    public void record(
            long playerTick,
            String playerName,
            ResourceLocation event,
            TriggerBinding binding,
            TriggerTraceEntry.Outcome outcome,
            double effectiveChance,
            int appliedActions
    ) {
        record(playerTick, playerName, event, binding, outcome, effectiveChance,
                appliedActions, appliedActions, List.of());
    }

    public synchronized List<TriggerTraceEntry> recent(int maximum) {
        int count = Math.max(0, Math.min(maximum, entries.size()));
        ArrayList<TriggerTraceEntry> result = new ArrayList<>(entries);
        Collections.reverse(result);
        return List.copyOf(result.subList(0, count));
    }

    public synchronized void clear() {
        entries.clear();
        overwritten = 0L;
        nextSequence = 1L;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(enabled, entries.size(), capacity, overwritten);
    }

    public record Snapshot(boolean enabled, int size, int capacity, long overwritten) {
    }
}
