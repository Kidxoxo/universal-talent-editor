package com.kidxoxo.talenteditor.counter;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.CounterEventType;
import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.domain.CounterResetTrigger;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import com.kidxoxo.talenteditor.runtime.TalentEventTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

public final class CounterEventService {
    private static final long SYNC_INTERVAL_TICKS = 20L;
    private static final long FULL_SYNC_INTERVAL_TICKS = 100L;
    private final Map<UUID, Map<ResourceLocation, Long>> lastAcceptedTicks = new HashMap<>();
    private final Map<UUID, Set<ResourceLocation>> dirtyCounters = new HashMap<>();
    private final Set<UUID> fullStateDirty = new HashSet<>();
    private final Map<UUID, Long> lastSyncTicks = new HashMap<>();
    private final Map<UUID, Long> lastFullSyncTicks = new HashMap<>();
    private final Map<UUID, MovementSample> movementSamples = new HashMap<>();
    private long resetRuleRevision = Long.MIN_VALUE;
    private List<CounterRuleDefinition> loginResetRules = List.of();
    private List<CounterRuleDefinition> deathResetRules = List.of();
    private List<CounterRuleDefinition> periodicResetRules = List.of();

    public void record(
            CounterEventType event,
            ServerPlayer player,
            Entity relatedEntity,
            ItemStack relatedItem,
            DamageSource damageSource,
            double eventAmount
    ) {
        var rules = TalentEditorMod.SNAPSHOTS.active().countersByEvent().get(event);
        if (rules == null || rules.isEmpty()) return;
        CounterRuleEngine.EventContext context = context(player, relatedEntity, relatedItem, damageSource);
        long gameTime = player.serverLevel().getGameTime();
        for (CounterRuleDefinition rule : rules) {
            if (!CounterRuleEngine.matches(rule, context) || coolingDown(player.getUUID(), rule, gameTime)) continue;
            long current = PlayerTalents.counter(player, rule.id());
            long next = CounterRuleEngine.nextValue(rule, current, eventAmount);
            if (next == current) continue;
            PlayerTalents.setCounter(player, rule.id(), next);
            lastAcceptedTicks.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                    .put(rule.id(), gameTime);
            markDirty(player.getUUID(), rule.id(), gameTime);
            TalentEditorMod.TRIGGERS.fireAmount(
                    TalentEventTypes.COUNTER_CHANGED, player, relatedEntity, damageSource, next
            );
        }
    }

    public void playerTick(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        double movementDistance = movementDistance(player);
        if (movementDistance > 0.0D) {
            record(CounterEventType.MOVE_DISTANCE, player, null, ItemStack.EMPTY, null, movementDistance);
            TalentEditorMod.TRIGGERS.fireMovement(player, movementDistance);
        }
        long lifecycleTick = PlayerTalents.advanceCounterLifecycleTick(player);
        refreshResetRules();
        if (lifecycleTick > 0L) {
            for (CounterRuleDefinition rule : periodicResetRules) {
                if (rule.periodicResetDue(lifecycleTick) && resetCounter(player, rule.id())) {
                    markDirty(player.getUUID(), rule.id(), gameTime);
                }
            }
        }
        var timed = TalentEditorMod.SNAPSHOTS.active().countersByEvent().get(CounterEventType.PLAY_TIME);
        if (timed != null) {
            for (CounterRuleDefinition rule : timed) {
                if (gameTime % rule.intervalTicks() == 0L) {
                    recordSingle(rule, player, gameTime);
                }
            }
        }
        flushIfDue(player, gameTime);
    }

    public void removePlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        lastAcceptedTicks.remove(id);
        dirtyCounters.remove(id);
        fullStateDirty.remove(id);
        lastSyncTicks.remove(id);
        lastFullSyncTicks.remove(id);
        movementSamples.remove(id);
    }

    public boolean resetForTrigger(ServerPlayer player, CounterResetTrigger trigger) {
        refreshResetRules();
        List<CounterRuleDefinition> rules = trigger == CounterResetTrigger.LOGIN
                ? loginResetRules : deathResetRules;
        boolean changed = false;
        for (CounterRuleDefinition rule : rules) changed |= resetCounter(player, rule.id());
        return changed;
    }

    public boolean resetCounter(ServerPlayer player, ResourceLocation counter) {
        if (PlayerTalents.counter(player, counter) == 0L) return false;
        PlayerTalents.setCounter(player, counter, 0L);
        TalentEditorMod.TRIGGERS.fireAmount(
                TalentEventTypes.COUNTER_CHANGED, player, null, null, 0.0D
        );
        Map<ResourceLocation, Long> cooldowns = lastAcceptedTicks.get(player.getUUID());
        if (cooldowns != null) {
            cooldowns.remove(counter);
            if (cooldowns.isEmpty()) lastAcceptedTicks.remove(player.getUUID());
        }
        return true;
    }

    /** Batches progress updates made by actions outside the automatic counter event pipeline. */
    public void markExternalChange(ServerPlayer player, ResourceLocation counter) {
        markDirty(player.getUUID(), counter, player.serverLevel().getGameTime());
        TalentEditorMod.TRIGGERS.fireAmount(
                TalentEventTypes.COUNTER_CHANGED, player, null, null, PlayerTalents.counter(player, counter)
        );
    }

    private void refreshResetRules() {
        var snapshot = TalentEditorMod.SNAPSHOTS.active();
        if (snapshot.revision() == resetRuleRevision) return;
        List<CounterRuleDefinition> active = snapshot.counters().values().stream()
                .filter(CounterRuleDefinition::enabled)
                .filter(CounterRuleDefinition::available)
                .toList();
        loginResetRules = active.stream().filter(rule -> rule.resetsOn(CounterResetTrigger.LOGIN)).toList();
        deathResetRules = active.stream().filter(rule -> rule.resetsOn(CounterResetTrigger.DEATH)).toList();
        periodicResetRules = active.stream().filter(rule -> rule.resetIntervalTicks() > 0).toList();
        resetRuleRevision = snapshot.revision();
    }

    private void recordSingle(CounterRuleDefinition rule, ServerPlayer player, long gameTime) {
        CounterRuleEngine.EventContext context = new CounterRuleEngine.EventContext(
                PlayerTalents.owned(player), Optional.empty(), Set.of(), Optional.empty(), Set.of(), Set.of()
        );
        if (!CounterRuleEngine.matches(rule, context) || coolingDown(player.getUUID(), rule, gameTime)) return;
        long current = PlayerTalents.counter(player, rule.id());
        long next = CounterRuleEngine.nextValue(rule, current, 0.0D);
        if (next == current) return;
        PlayerTalents.setCounter(player, rule.id(), next);
        lastAcceptedTicks.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(rule.id(), gameTime);
        markDirty(player.getUUID(), rule.id(), gameTime);
        TalentEditorMod.TRIGGERS.fireAmount(
                TalentEventTypes.COUNTER_CHANGED, player, null, null, next
        );
    }

    private boolean coolingDown(UUID player, CounterRuleDefinition rule, long gameTime) {
        if (rule.cooldownTicks() <= 0) return false;
        Long last = lastAcceptedTicks.getOrDefault(player, Map.of()).get(rule.id());
        return last != null && gameTime >= last && gameTime - last < rule.cooldownTicks();
    }

    private void flushIfDue(ServerPlayer player, long gameTime) {
        UUID id = player.getUUID();
        Set<ResourceLocation> dirty = dirtyCounters.get(id);
        if (dirty != null && !dirty.isEmpty()) {
            long last = lastSyncTicks.getOrDefault(id, Long.MIN_VALUE);
            if (last == Long.MIN_VALUE || gameTime < last || gameTime - last >= SYNC_INTERVAL_TICKS) {
                EditorNetwork.sendCounterProgress(player, dirty);
                dirtyCounters.remove(id);
                lastSyncTicks.put(id, gameTime);
            }
        }
        if (fullStateDirty.contains(id)) {
            long lastFull = lastFullSyncTicks.getOrDefault(id, gameTime);
            if (gameTime < lastFull || gameTime - lastFull >= FULL_SYNC_INTERVAL_TICKS) {
                EditorNetwork.sendPlayerState(player, false);
                fullStateDirty.remove(id);
                lastFullSyncTicks.put(id, gameTime);
            }
        }
    }

    private void markDirty(UUID player, ResourceLocation counter, long gameTime) {
        dirtyCounters.computeIfAbsent(player, ignored -> new HashSet<>()).add(counter);
        if (fullStateDirty.add(player)) lastFullSyncTicks.put(player, gameTime);
    }

    private static CounterRuleEngine.EventContext context(
            ServerPlayer player,
            Entity entity,
            ItemStack stack,
            DamageSource source
    ) {
        ResourceLocation entityType = entity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        Set<ResourceLocation> entityTags = entity == null ? Set.of() : entity.getType().builtInRegistryHolder()
                .tags().map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        ItemStack effectiveStack = stack == null ? ItemStack.EMPTY : stack;
        ResourceLocation item = effectiveStack.isEmpty() ? null : ConfiguredItem.definitionId(effectiveStack)
                .orElseGet(() -> ForgeRegistries.ITEMS.getKey(effectiveStack.getItem()));
        Set<ResourceLocation> itemTags = effectiveStack.isEmpty() ? Set.of() : effectiveStack.getItem()
                .builtInRegistryHolder().tags().map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        Set<ResourceLocation> damageTags = source == null ? Set.of() : source.typeHolder().tags()
                .map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        return new CounterRuleEngine.EventContext(
                PlayerTalents.owned(player), Optional.ofNullable(entityType), entityTags,
                Optional.ofNullable(item), itemTags, damageTags
        );
    }

    private double movementDistance(ServerPlayer player) {
        ResourceLocation dimension = player.serverLevel().dimension().location();
        MovementSample current = new MovementSample(dimension, player.getX(), player.getZ());
        MovementSample previous = movementSamples.put(player.getUUID(), current);
        if (previous == null || !previous.dimension().equals(dimension)) return 0.0D;
        double dx = current.x() - previous.x();
        double dz = current.z() - previous.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        // Ignore position jumps such as commands, portals, and respawn placement.
        return Double.isFinite(distance) && distance >= 0.001D && distance <= 64.0D ? distance : 0.0D;
    }

    private record MovementSample(ResourceLocation dimension, double x, double z) {
    }
}
