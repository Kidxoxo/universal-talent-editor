package com.kidxoxo.talenteditor.player;

import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import com.kidxoxo.talenteditor.domain.DrawConstraintQueueMode;
import com.kidxoxo.talenteditor.domain.DrawConstraintSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlayerTalentData implements INBTSerializable<CompoundTag> {
    private static final String TALENTS_KEY = "talents";
    private static final String TALENT_HISTORY_KEY = "talentHistory";
    private static final String RETAINED_EFFECTS_KEY = "retainedEffects";
    private static final String RETAINED_CATEGORIES_KEY = "retainedCategories";
    private static final String RETAINED_TRIGGERS_KEY = "retainedTriggers";
    private static final String STATES_KEY = "numericStates";
    private static final String STATE_ID_KEY = "id";
    private static final String STATE_VALUE_KEY = "value";
    private static final String ACQUISITIONS_KEY = "talentAcquisitions";
    private static final String ACQUISITION_ID_KEY = "id";
    private static final String ACQUISITION_TICK_KEY = "tick";
    private static final String ACQUISITION_DRAW_KEY = "draw";
    private static final String DRAW_SELECTIONS_KEY = "drawSelections";
    private static final String DRAW_TOKENS_KEY = "drawTokens";
    private static final String COUNTERS_KEY = "counters";
    private static final String COUNTER_ID_KEY = "id";
    private static final String COUNTER_VALUE_KEY = "value";
    private static final String PITY_KEY = "drawPity";
    private static final String PITY_POOL_KEY = "pool";
    private static final String PITY_VALUE_KEY = "value";
    private static final String COUNTER_LIFECYCLE_TICKS_KEY = "counterLifecycleTicks";
    private static final String TRIGGER_COOLDOWNS_KEY = "triggerCooldowns";
    private static final String TRIGGER_COOLDOWN_ID_KEY = "id";
    private static final String TRIGGER_COOLDOWN_TICK_KEY = "tick";
    private static final String TRIGGER_ACTIVATIONS_KEY = "triggerActivations";
    private static final String TRIGGER_ACTIVATION_ID_KEY = "id";
    private static final String TRIGGER_ACTIVATION_COUNT_KEY = "count";
    private static final String TRIGGER_DISTANCE_PROGRESS_KEY = "triggerDistanceProgress";
    private static final String TRIGGER_DISTANCE_ID_KEY = "id";
    private static final String TRIGGER_DISTANCE_VALUE_KEY = "distance";
    private static final String DRAW_CONSTRAINTS_KEY = "drawConstraints";
    private static final String DRAW_CONSTRAINT_TALENTS_KEY = "talents";
    private static final String DRAW_CONSTRAINT_CATEGORIES_KEY = "categories";
    private static final String DRAW_CONSTRAINT_RARITIES_KEY = "rarities";
    private static final String DRAW_CONSTRAINT_TAGS_KEY = "tags";
    private static final String DRAW_CONSTRAINT_POOLS_KEY = "pools";
    private static final String DRAW_CONSTRAINT_GUARANTEED_KEY = "guaranteed";
    public static final int MAX_DRAW_TOKENS = 1_000_000;
    public static final int MAX_COUNTERS = 4_096;
    public static final long MAX_COUNTER_VALUE = 1_000_000_000_000L;
    public static final int MAX_PITY_POOLS = 2_048;
    public static final int MAX_PITY_VALUE = 1_000_000;
    public static final int MAX_TRIGGER_COOLDOWNS = 4_096;
    public static final int MAX_TRIGGER_COOLDOWN_ID_LENGTH = 512;
    public static final int MAX_TRIGGER_ACTIVATIONS = 4_096;
    public static final int MAX_TRIGGER_ACTIVATION_COUNT = 1_000_000;
    public static final int MAX_TRIGGER_DISTANCE_PROGRESS = 4_096;
    public static final double MAX_TRIGGER_DISTANCE_VALUE = 1_000_000_000_000.0D;
    public static final int MAX_DRAW_CONSTRAINTS = 64;
    public static final int MAX_NUMERIC_STATES = 4_096;
    public static final double MAX_NUMERIC_STATE_VALUE = 1.0E12D;
    private static final int MAX_DRAW_CONSTRAINT_VALUES = 256;
    private static final int MAX_TALENT_HISTORY = 10_000;
    private final LinkedHashSet<ResourceLocation> owned = new LinkedHashSet<>();
    private final LinkedHashSet<ResourceLocation> talentHistory = new LinkedHashSet<>();
    private final LinkedHashSet<ResourceLocation> retainedEffects = new LinkedHashSet<>();
    private final LinkedHashSet<ResourceLocation> retainedCategories = new LinkedHashSet<>();
    private final LinkedHashSet<ResourceLocation> retainedTriggers = new LinkedHashSet<>();
    private final LinkedHashMap<ResourceLocation, Double> numericStates = new LinkedHashMap<>();
    private final LinkedHashMap<ResourceLocation, Acquisition> acquisitions = new LinkedHashMap<>();
    private final LinkedHashMap<ResourceLocation, Long> counters = new LinkedHashMap<>();
    private final LinkedHashMap<ResourceLocation, Integer> drawPity = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> triggerCooldowns = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> triggerActivations = new LinkedHashMap<>();
    private final LinkedHashMap<String, Double> triggerDistanceProgress = new LinkedHashMap<>();
    private final ArrayList<QueueDrawConstraintAction> drawConstraints = new ArrayList<>();
    private int drawTokens;
    private long counterLifecycleTicks;
    private long drawSelections;

    public Set<ResourceLocation> owned() {
        return Set.copyOf(owned);
    }

    public boolean contains(ResourceLocation talent) {
        return owned.contains(talent);
    }

    public boolean grant(ResourceLocation talent) {
        if (!owned.add(talent)) return false;
        talentHistory.add(talent);
        retainedEffects.remove(talent);
        retainedCategories.remove(talent);
        retainedTriggers.remove(talent);
        acquisitions.put(talent, new Acquisition(counterLifecycleTicks, drawSelections));
        return true;
    }

    public boolean revoke(ResourceLocation talent) {
        if (!owned.remove(talent)) return false;
        retainedEffects.remove(talent);
        retainedCategories.remove(talent);
        retainedTriggers.remove(talent);
        acquisitions.remove(talent);
        clearTriggerState(talent);
        return true;
    }

    public boolean expire(
            ResourceLocation talent,
            boolean retainEffects,
            boolean retainCategory,
            boolean retainTriggerBindings
    ) {
        if (!owned.remove(talent)) return false;
        if (retainEffects) retainedEffects.add(talent); else retainedEffects.remove(talent);
        if (retainCategory) retainedCategories.add(talent); else retainedCategories.remove(talent);
        if (retainTriggerBindings) retainedTriggers.add(talent); else retainedTriggers.remove(talent);
        acquisitions.remove(talent);
        if (!retainTriggerBindings) clearTriggerState(talent);
        return true;
    }

    /** Backward-compatible expiry API; older callers do not retain triggers. */
    public boolean expire(ResourceLocation talent, boolean retainEffects, boolean retainCategory) {
        return expire(talent, retainEffects, retainCategory, false);
    }

    public Set<ResourceLocation> activeSources() {
        LinkedHashSet<ResourceLocation> values = new LinkedHashSet<>(owned);
        values.addAll(retainedEffects);
        return Set.copyOf(values);
    }

    public Set<ResourceLocation> categorySources() {
        LinkedHashSet<ResourceLocation> values = new LinkedHashSet<>(owned);
        values.addAll(retainedCategories);
        return Set.copyOf(values);
    }

    public Set<ResourceLocation> triggerSources() {
        LinkedHashSet<ResourceLocation> values = new LinkedHashSet<>(owned);
        values.addAll(retainedTriggers);
        return Set.copyOf(values);
    }

    public Set<ResourceLocation> talentHistory() {
        return Set.copyOf(talentHistory);
    }

    public boolean hasEverAcquired(ResourceLocation talent) {
        return talentHistory.contains(talent);
    }

    public double numericState(ResourceLocation id) {
        return numericStates.getOrDefault(id, 0.0D);
    }

    public Map<ResourceLocation, Double> numericStates() {
        return Map.copyOf(numericStates);
    }

    public double setNumericState(ResourceLocation id, double value) {
        double bounded = Double.isFinite(value)
                ? Math.max(-MAX_NUMERIC_STATE_VALUE, Math.min(MAX_NUMERIC_STATE_VALUE, value)) : 0.0D;
        if (Math.abs(bounded) < 1.0E-12D) {
            numericStates.remove(id);
        } else if (numericStates.containsKey(id) || numericStates.size() < MAX_NUMERIC_STATES) {
            numericStates.put(id, bounded);
        }
        return numericState(id);
    }

    public long acquiredTick(ResourceLocation talent) {
        Acquisition acquisition = acquisitions.get(talent);
        return acquisition == null ? -1L : acquisition.tick();
    }

    public long acquiredDraw(ResourceLocation talent) {
        Acquisition acquisition = acquisitions.get(talent);
        return acquisition == null ? -1L : acquisition.draw();
    }

    public long drawSelections() {
        return drawSelections;
    }

    public long recordDrawSelection() {
        drawSelections = drawSelections == Long.MAX_VALUE ? 1L : drawSelections + 1L;
        return drawSelections;
    }

    public Map<ResourceLocation, Long> counters() {
        return Map.copyOf(counters);
    }

    public long counter(ResourceLocation id) {
        return counters.getOrDefault(id, 0L);
    }

    public long setCounter(ResourceLocation id, long value) {
        long bounded = Math.max(0L, Math.min(MAX_COUNTER_VALUE, value));
        if (bounded == 0L) {
            counters.remove(id);
        } else if (counters.containsKey(id) || counters.size() < MAX_COUNTERS) {
            counters.put(id, bounded);
        }
        return counter(id);
    }

    public long addCounter(ResourceLocation id, long amount) {
        long current = counter(id);
        long result;
        if (amount > 0L && current > MAX_COUNTER_VALUE - amount) {
            result = MAX_COUNTER_VALUE;
        } else if (amount < 0L && amount <= -current) {
            result = 0L;
        } else {
            result = current + amount;
        }
        return setCounter(id, result);
    }

    public long counterLifecycleTicks() {
        return counterLifecycleTicks;
    }

    public long advanceCounterLifecycleTick() {
        counterLifecycleTicks = counterLifecycleTicks == Long.MAX_VALUE ? 1L : counterLifecycleTicks + 1L;
        return counterLifecycleTicks;
    }

    public long triggerLastActivation(String id) {
        if (!validTriggerCooldownId(id)) return -1L;
        return triggerCooldowns.getOrDefault(id, -1L);
    }

    public boolean setTriggerLastActivation(String id, long tick) {
        if (!validTriggerCooldownId(id) || tick < 0L) return false;
        if (!triggerCooldowns.containsKey(id) && triggerCooldowns.size() >= MAX_TRIGGER_COOLDOWNS) return false;
        triggerCooldowns.put(id, tick);
        return true;
    }

    public int triggerActivationCount(String id) {
        if (!validTriggerStateId(id)) return 0;
        return triggerActivations.getOrDefault(id, 0);
    }

    public boolean canRecordTriggerActivation(String id) {
        return validTriggerStateId(id)
                && (triggerActivations.containsKey(id) || triggerActivations.size() < MAX_TRIGGER_ACTIVATIONS);
    }

    public boolean incrementTriggerActivation(String id) {
        if (!canRecordTriggerActivation(id)) return false;
        int next = Math.min(MAX_TRIGGER_ACTIVATION_COUNT, triggerActivationCount(id) + 1);
        triggerActivations.put(id, next);
        return true;
    }

    public void copyFrom(PlayerTalentData other) {
        owned.clear();
        owned.addAll(other.owned);
        talentHistory.clear();
        talentHistory.addAll(other.talentHistory);
        retainedEffects.clear();
        retainedEffects.addAll(other.retainedEffects);
        retainedCategories.clear();
        retainedCategories.addAll(other.retainedCategories);
        retainedTriggers.clear();
        retainedTriggers.addAll(other.retainedTriggers);
        numericStates.clear();
        numericStates.putAll(other.numericStates);
        acquisitions.clear();
        acquisitions.putAll(other.acquisitions);
        counters.clear();
        counters.putAll(other.counters);
        drawPity.clear();
        drawPity.putAll(other.drawPity);
        triggerCooldowns.clear();
        triggerCooldowns.putAll(other.triggerCooldowns);
        triggerActivations.clear();
        triggerActivations.putAll(other.triggerActivations);
        triggerDistanceProgress.clear();
        triggerDistanceProgress.putAll(other.triggerDistanceProgress);
        migratedItemTriggerSources.clear();
        migratedItemTriggerSources.addAll(other.migratedItemTriggerSources);
        drawConstraints.clear();
        drawConstraints.addAll(other.drawConstraints);
        drawTokens = other.drawTokens;
        counterLifecycleTicks = other.counterLifecycleTicks;
        drawSelections = other.drawSelections;
    }

    public int drawTokens() {
        return drawTokens;
    }

    public double triggerDistanceProgress(String id) {
        if (!validTriggerStateId(id)) return 0.0D;
        return triggerDistanceProgress.getOrDefault(id, 0.0D);
    }

    /** Adds horizontal movement and claims at most one configured interval, preserving any remainder. */
    public boolean claimTriggerDistanceInterval(String id, double distance, double interval) {
        if (!validTriggerStateId(id) || !Double.isFinite(distance) || distance <= 0.0D
                || !Double.isFinite(interval) || interval <= 0.0D) return false;
        if (!triggerDistanceProgress.containsKey(id)
                && triggerDistanceProgress.size() >= MAX_TRIGGER_DISTANCE_PROGRESS) return false;
        double current = triggerDistanceProgress(id);
        double total = Math.min(MAX_TRIGGER_DISTANCE_VALUE, current + distance);
        if (total + 1.0E-9D < interval) {
            triggerDistanceProgress.put(id, total);
            return false;
        }
        double remainder = total % interval;
        if (remainder <= 1.0E-9D) triggerDistanceProgress.remove(id);
        else triggerDistanceProgress.put(id, remainder);
        return true;
    }

    public List<QueueDrawConstraintAction> drawConstraints() {
        return List.copyOf(drawConstraints);
    }

    public boolean queueDrawConstraint(QueueDrawConstraintAction constraint) {
        return queueDrawConstraint(constraint, Map.of());
    }

    public boolean queueDrawConstraint(QueueDrawConstraintAction constraint,
                                       Map<ResourceLocation, Integer> poolChoices) {
        if (constraint == null) return false;
        long previous = constraint.successCounter().map(this::counter).orElse(0L);
        if (constraint.successCounter().isPresent()
                && previous == 0L
                && !counters.containsKey(constraint.successCounter().orElseThrow())
                && counters.size() >= MAX_COUNTERS) {
            return false;
        }
        long next = constraint.nextSuccessCount(previous);
        DrawConstraintSpec selected = constraint.constraintForSuccess(previous, next);
        QueueDrawConstraintAction pending = QueueDrawConstraintAction.pending(selected);
        if (constraint.queueMode() == DrawConstraintQueueMode.DEDUPE
                && drawConstraints.stream().anyMatch(value -> value.spec().equals(selected))) {
            return false;
        }
        for (var entry : poolChoices.entrySet()) {
            if (!selected.appliesTo(entry.getKey())) continue;
            int occupied = drawConstraints.stream().filter(value -> value.appliesTo(entry.getKey()))
                    .mapToInt(QueueDrawConstraintAction::guaranteed).sum();
            int capacity = entry.getValue();
            int reserved = Math.min(selected.guaranteed(), capacity);
            if (occupied >= capacity || (constraint.queueMode() != DrawConstraintQueueMode.REPLACE
                    && occupied + reserved > capacity)) {
                // The search itself succeeds even when this draw has no free choice slot.
                // Keep the earlier reservations intact, but advance its success counter.
                constraint.successCounter().ifPresent(counter -> setCounter(counter, next));
                return true;
            }
        }
        if (constraint.queueMode() == DrawConstraintQueueMode.REPLACE) {
            drawConstraints.removeIf(value -> value.spec().overlapsPoolScope(selected));
        }
        if (drawConstraints.size() >= MAX_DRAW_CONSTRAINTS) return false;
        drawConstraints.add(pending);
        constraint.successCounter().ifPresent(counter -> setCounter(counter, next));
        return true;
    }

    public Optional<QueueDrawConstraintAction> nextDrawConstraint(ResourceLocation pool) {
        return drawConstraints.stream().filter(value -> value.appliesTo(pool)).findFirst();
    }

    public List<QueueDrawConstraintAction> drawConstraintsForPool(ResourceLocation pool, int choices) {
        ArrayList<QueueDrawConstraintAction> result = new ArrayList<>();
        int slots = 0;
        for (QueueDrawConstraintAction value : drawConstraints) {
            if (!value.appliesTo(pool)) continue;
            if (slots >= choices) break;
            result.add(value);
            slots += value.guaranteed();
        }
        return List.copyOf(result);
    }

    public boolean consumeNextDrawConstraint(ResourceLocation pool) {
        for (int index = 0; index < drawConstraints.size(); index++) {
            if (drawConstraints.get(index).appliesTo(pool)) {
                drawConstraints.remove(index);
                return true;
            }
        }
        return false;
    }

    public void setDrawTokens(int value) {
        drawTokens = Math.max(0, Math.min(MAX_DRAW_TOKENS, value));
    }

    public int addDrawTokens(int amount) {
        long result = (long) drawTokens + amount;
        setDrawTokens((int) Math.max(0L, Math.min(MAX_DRAW_TOKENS, result)));
        return drawTokens;
    }

    public boolean consumeDrawToken() {
        if (drawTokens <= 0) return false;
        drawTokens--;
        return true;
    }

    public boolean acquireWithDrawToken(ResourceLocation talent) {
        if (drawTokens <= 0 || owned.contains(talent)) return false;
        drawTokens--;
        return grant(talent);
    }

    public int drawPity(ResourceLocation pool) {
        return drawPity.getOrDefault(pool, 0);
    }

    public Map<ResourceLocation, Integer> drawPity() {
        return Map.copyOf(drawPity);
    }

    public int recordDrawPity(ResourceLocation pool, boolean satisfied, int maximum) {
        if (satisfied || maximum <= 0) {
            drawPity.remove(pool);
            return 0;
        }
        int boundedMaximum = Math.min(MAX_PITY_VALUE, maximum);
        int next = Math.min(boundedMaximum, drawPity(pool) + 1);
        if (drawPity.containsKey(pool) || drawPity.size() < MAX_PITY_POOLS) drawPity.put(pool, next);
        return drawPity(pool);
    }

    public boolean exchangeDrawTokenForCounter(ResourceLocation counter, long amount) {
        if (drawTokens <= 0 || amount <= 0L) return false;
        long current = counter(counter);
        if (current >= MAX_COUNTER_VALUE || (!counters.containsKey(counter) && counters.size() >= MAX_COUNTERS)) {
            return false;
        }
        drawTokens--;
        addCounter(counter, amount);
        return true;
    }

    public boolean applySynthesis(
            ResourceLocation result,
            Set<ResourceLocation> consumeTalents,
            Map<ResourceLocation, Long> consumeCounters
    ) {
        if (owned.contains(result) || !owned.containsAll(consumeTalents)) return false;
        for (Map.Entry<ResourceLocation, Long> cost : consumeCounters.entrySet()) {
            if (cost.getValue() < 0L || counter(cost.getKey()) < cost.getValue()) return false;
        }
        consumeTalents.forEach(this::revoke);
        consumeCounters.forEach((id, amount) -> setCounter(id, counter(id) - amount));
        return grant(result);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        ListTag values = new ListTag();
        owned.stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(id -> values.add(StringTag.valueOf(id.toString())));
        root.put(TALENTS_KEY, values);
        putTalentIds(root, TALENT_HISTORY_KEY, talentHistory);
        putTalentIds(root, RETAINED_EFFECTS_KEY, retainedEffects);
        putTalentIds(root, RETAINED_CATEGORIES_KEY, retainedCategories);
        putTalentIds(root, RETAINED_TRIGGERS_KEY, retainedTriggers);
        root.putInt(DRAW_TOKENS_KEY, drawTokens);
        root.putLong(COUNTER_LIFECYCLE_TICKS_KEY, counterLifecycleTicks);
        root.putLong(DRAW_SELECTIONS_KEY, drawSelections);
        ListTag stateValues = new ListTag();
        numericStates.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    CompoundTag state = new CompoundTag();
                    state.putString(STATE_ID_KEY, entry.getKey().toString());
                    state.putDouble(STATE_VALUE_KEY, entry.getValue());
                    stateValues.add(state);
                });
        root.put(STATES_KEY, stateValues);
        ListTag acquisitionValues = new ListTag();
        acquisitions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    CompoundTag acquisition = new CompoundTag();
                    acquisition.putString(ACQUISITION_ID_KEY, entry.getKey().toString());
                    acquisition.putLong(ACQUISITION_TICK_KEY, entry.getValue().tick());
                    acquisition.putLong(ACQUISITION_DRAW_KEY, entry.getValue().draw());
                    acquisitionValues.add(acquisition);
                });
        root.put(ACQUISITIONS_KEY, acquisitionValues);
        ListTag counterValues = new ListTag();
        counters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    CompoundTag counter = new CompoundTag();
                    counter.putString(COUNTER_ID_KEY, entry.getKey().toString());
                    counter.putLong(COUNTER_VALUE_KEY, entry.getValue());
                    counterValues.add(counter);
                });
        root.put(COUNTERS_KEY, counterValues);
        ListTag pityValues = new ListTag();
        drawPity.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    CompoundTag pity = new CompoundTag();
                    pity.putString(PITY_POOL_KEY, entry.getKey().toString());
                    pity.putInt(PITY_VALUE_KEY, entry.getValue());
                    pityValues.add(pity);
                });
        root.put(PITY_KEY, pityValues);
        ListTag cooldownValues = new ListTag();
        triggerCooldowns.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag cooldown = new CompoundTag();
                    cooldown.putString(TRIGGER_COOLDOWN_ID_KEY, entry.getKey());
                    cooldown.putLong(TRIGGER_COOLDOWN_TICK_KEY, entry.getValue());
                    cooldownValues.add(cooldown);
                });
        root.put(TRIGGER_COOLDOWNS_KEY, cooldownValues);
        ListTag activationValues = new ListTag();
        triggerActivations.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag activation = new CompoundTag();
                    activation.putString(TRIGGER_ACTIVATION_ID_KEY, entry.getKey());
                    activation.putInt(TRIGGER_ACTIVATION_COUNT_KEY, entry.getValue());
                    activationValues.add(activation);
                });
        root.put(TRIGGER_ACTIVATIONS_KEY, activationValues);
        ListTag distanceValues = new ListTag();
        triggerDistanceProgress.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CompoundTag progress = new CompoundTag();
                    progress.putString(TRIGGER_DISTANCE_ID_KEY, entry.getKey());
                    progress.putDouble(TRIGGER_DISTANCE_VALUE_KEY, entry.getValue());
                    distanceValues.add(progress);
                });
        root.put(TRIGGER_DISTANCE_PROGRESS_KEY, distanceValues);
        ListTag constraintValues = new ListTag();
        drawConstraints.forEach(value -> {
            CompoundTag constraint = new CompoundTag();
            putIds(constraint, DRAW_CONSTRAINT_TALENTS_KEY, value.talents());
            putIds(constraint, DRAW_CONSTRAINT_CATEGORIES_KEY, value.categories());
            putIds(constraint, DRAW_CONSTRAINT_RARITIES_KEY, value.rarities());
            putIds(constraint, DRAW_CONSTRAINT_TAGS_KEY, value.tags());
            putIds(constraint, DRAW_CONSTRAINT_POOLS_KEY, value.pools());
            constraint.putInt(DRAW_CONSTRAINT_GUARANTEED_KEY, value.guaranteed());
            constraintValues.add(constraint);
        });
        root.put(DRAW_CONSTRAINTS_KEY, constraintValues);
        return root;
    }

    @Override
    public void deserializeNBT(CompoundTag root) {
        migratedItemTriggerSources.clear();
        owned.clear();
        ListTag values = root.getList(TALENTS_KEY, Tag.TAG_STRING);
        for (int index = 0; index < values.size(); index++) {
            ResourceLocation id = ResourceLocation.tryParse(values.getString(index));
            if (id != null) owned.add(id);
        }
        talentHistory.clear();
        talentHistory.addAll(readTalentIds(root, TALENT_HISTORY_KEY));
        if (talentHistory.isEmpty()) talentHistory.addAll(owned);
        retainedEffects.clear();
        retainedEffects.addAll(readTalentIds(root, RETAINED_EFFECTS_KEY));
        retainedCategories.clear();
        retainedCategories.addAll(readTalentIds(root, RETAINED_CATEGORIES_KEY));
        retainedTriggers.clear();
        retainedTriggers.addAll(readTalentIds(root, RETAINED_TRIGGERS_KEY));
        numericStates.clear();
        ListTag stateValues = root.getList(STATES_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < stateValues.size() && numericStates.size() < MAX_NUMERIC_STATES; index++) {
            CompoundTag state = stateValues.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(state.getString(STATE_ID_KEY));
            double value = state.getDouble(STATE_VALUE_KEY);
            if (id != null && Double.isFinite(value)) setNumericState(id, value);
        }
        acquisitions.clear();
        ListTag acquisitionValues = root.getList(ACQUISITIONS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < acquisitionValues.size(); index++) {
            CompoundTag acquisition = acquisitionValues.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(acquisition.getString(ACQUISITION_ID_KEY));
            if (id != null && owned.contains(id)) acquisitions.put(id, new Acquisition(
                    Math.max(0L, acquisition.getLong(ACQUISITION_TICK_KEY)),
                    Math.max(0L, acquisition.getLong(ACQUISITION_DRAW_KEY))
            ));
        }
        counters.clear();
        ListTag counterValues = root.getList(COUNTERS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < counterValues.size() && counters.size() < MAX_COUNTERS; index++) {
            CompoundTag counter = counterValues.getCompound(index);
            ResourceLocation id = ResourceLocation.tryParse(counter.getString(COUNTER_ID_KEY));
            long value = counter.getLong(COUNTER_VALUE_KEY);
            if (id != null && value > 0L) setCounter(id, value);
        }
        drawPity.clear();
        ListTag pityValues = root.getList(PITY_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < pityValues.size() && drawPity.size() < MAX_PITY_POOLS; index++) {
            CompoundTag pity = pityValues.getCompound(index);
            ResourceLocation pool = ResourceLocation.tryParse(pity.getString(PITY_POOL_KEY));
            int value = pity.getInt(PITY_VALUE_KEY);
            if (pool != null && value > 0) drawPity.put(pool, Math.min(MAX_PITY_VALUE, value));
        }
        triggerCooldowns.clear();
        ListTag cooldownValues = root.getList(TRIGGER_COOLDOWNS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < cooldownValues.size()
                && triggerCooldowns.size() < MAX_TRIGGER_COOLDOWNS; index++) {
            CompoundTag cooldown = cooldownValues.getCompound(index);
            setTriggerLastActivation(
                    cooldown.getString(TRIGGER_COOLDOWN_ID_KEY),
                    cooldown.getLong(TRIGGER_COOLDOWN_TICK_KEY)
            );
        }
        triggerActivations.clear();
        ListTag activationValues = root.getList(TRIGGER_ACTIVATIONS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < activationValues.size()
                && triggerActivations.size() < MAX_TRIGGER_ACTIVATIONS; index++) {
            CompoundTag activation = activationValues.getCompound(index);
            String id = activation.getString(TRIGGER_ACTIVATION_ID_KEY);
            int count = activation.getInt(TRIGGER_ACTIVATION_COUNT_KEY);
            if (validTriggerStateId(id) && count > 0) {
                triggerActivations.put(id, Math.min(MAX_TRIGGER_ACTIVATION_COUNT, count));
            }
        }
        triggerDistanceProgress.clear();
        ListTag distanceValues = root.getList(TRIGGER_DISTANCE_PROGRESS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < distanceValues.size()
                && triggerDistanceProgress.size() < MAX_TRIGGER_DISTANCE_PROGRESS; index++) {
            CompoundTag progress = distanceValues.getCompound(index);
            String id = progress.getString(TRIGGER_DISTANCE_ID_KEY);
            double value = progress.getDouble(TRIGGER_DISTANCE_VALUE_KEY);
            if (validTriggerStateId(id) && Double.isFinite(value) && value > 0.0D) {
                triggerDistanceProgress.put(id, Math.min(MAX_TRIGGER_DISTANCE_VALUE, value));
            }
        }
        drawConstraints.clear();
        ListTag constraintValues = root.getList(DRAW_CONSTRAINTS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < constraintValues.size()
                && drawConstraints.size() < MAX_DRAW_CONSTRAINTS; index++) {
            CompoundTag constraint = constraintValues.getCompound(index);
            List<ResourceLocation> talents = readIds(constraint, DRAW_CONSTRAINT_TALENTS_KEY);
            List<ResourceLocation> categories = readIds(constraint, DRAW_CONSTRAINT_CATEGORIES_KEY);
            List<ResourceLocation> rarities = readIds(constraint, DRAW_CONSTRAINT_RARITIES_KEY);
            List<ResourceLocation> tags = readIds(constraint, DRAW_CONSTRAINT_TAGS_KEY);
            if (talents.isEmpty() && categories.isEmpty() && rarities.isEmpty() && tags.isEmpty()) continue;
            int guaranteed = constraint.getInt(DRAW_CONSTRAINT_GUARANTEED_KEY);
            if (guaranteed < 1 || guaranteed > 5) continue;
            drawConstraints.add(new QueueDrawConstraintAction(
                    talents, categories, rarities, tags,
                    readIds(constraint, DRAW_CONSTRAINT_POOLS_KEY), guaranteed
            ));
        }
        setDrawTokens(root.getInt(DRAW_TOKENS_KEY));
        counterLifecycleTicks = Math.max(0L, root.getLong(COUNTER_LIFECYCLE_TICKS_KEY));
        drawSelections = Math.max(0L, root.getLong(DRAW_SELECTIONS_KEY));
        for (ResourceLocation talent : owned) {
            acquisitions.putIfAbsent(talent, new Acquisition(counterLifecycleTicks, drawSelections));
        }
    }

    private static boolean validTriggerCooldownId(String id) {
        return validTriggerStateId(id);
    }

    private static void putIds(CompoundTag parent, String key, List<ResourceLocation> values) {
        if (values.isEmpty()) return;
        ListTag list = new ListTag();
        values.stream().limit(MAX_DRAW_CONSTRAINT_VALUES)
                .forEach(value -> list.add(StringTag.valueOf(value.toString())));
        parent.put(key, list);
    }

    private static List<ResourceLocation> readIds(CompoundTag parent, String key) {
        ListTag values = parent.getList(key, Tag.TAG_STRING);
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        for (int index = 0; index < values.size() && result.size() < MAX_DRAW_CONSTRAINT_VALUES; index++) {
            ResourceLocation value = ResourceLocation.tryParse(values.getString(index));
            if (value != null) result.add(value);
        }
        return List.copyOf(result);
    }

    private static void putTalentIds(CompoundTag parent, String key, Set<ResourceLocation> values) {
        if (values.isEmpty()) return;
        ListTag list = new ListTag();
        values.stream().sorted(Comparator.comparing(ResourceLocation::toString)).limit(MAX_TALENT_HISTORY)
                .forEach(value -> list.add(StringTag.valueOf(value.toString())));
        parent.put(key, list);
    }

    private static List<ResourceLocation> readTalentIds(CompoundTag parent, String key) {
        ListTag values = parent.getList(key, Tag.TAG_STRING);
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        for (int index = 0; index < values.size() && result.size() < MAX_TALENT_HISTORY; index++) {
            ResourceLocation value = ResourceLocation.tryParse(values.getString(index));
            if (value != null) result.add(value);
        }
        return List.copyOf(result);
    }

    private final Set<ResourceLocation> migratedItemTriggerSources = new java.util.HashSet<>();

    /** Ambiguous legacy keys remain talent-owned; new item keys can never share talent state. */
    public void migrateItemTriggerState(ResourceLocation item, boolean legacyUnambiguous) {
        if (!migratedItemTriggerSources.add(item) || !legacyUnambiguous) return;
        String prefix = item + "#";
        migrateItemKeys(triggerCooldowns, prefix);
        migrateItemKeys(triggerActivations, prefix);
        migrateItemKeys(triggerDistanceProgress, prefix);
    }

    private static <T> void migrateItemKeys(Map<String, T> values, String prefix) {
        for (String key : new java.util.ArrayList<>(values.keySet())) {
            if (!key.startsWith(prefix)) continue;
            T value = values.remove(key);
            values.putIfAbsent("item|" + key, value);
        }
    }

    private void clearTriggerState(ResourceLocation talent) {
        String prefix = talent + "#";
        triggerCooldowns.keySet().removeIf(id -> id.startsWith(prefix));
        triggerActivations.keySet().removeIf(id -> id.startsWith(prefix));
        triggerDistanceProgress.keySet().removeIf(id -> id.startsWith(prefix));
    }

    private static boolean validTriggerStateId(String id) {
        return id != null && !id.isBlank() && id.length() <= MAX_TRIGGER_COOLDOWN_ID_LENGTH;
    }

    private record Acquisition(long tick, long draw) {
    }
}
