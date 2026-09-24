package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.CancelDamageAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ModifyDamageAction;
import com.kidxoxo.talenteditor.domain.RemoveEffectAction;
import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import com.kidxoxo.talenteditor.domain.ShowMessageAction;
import com.kidxoxo.talenteditor.domain.DamageEntityAction;
import com.kidxoxo.talenteditor.domain.HealEntityAction;
import com.kidxoxo.talenteditor.domain.LivingEntityTarget;
import com.kidxoxo.talenteditor.domain.ModifyEntityAttributeAction;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

public final class TalentTriggerService {
    private final ThreadLocal<Set<ExecutionKey>> activeExecutions = ThreadLocal.withInitial(HashSet::new);
    private final LongAdder eventCalls = new LongAdder();
    private final LongAdder matchedTriggers = new LongAdder();
    private final LongAdder appliedActions = new LongAdder();
    private final LongAdder cooldownSkips = new LongAdder();
    private final LongAdder chanceMisses = new LongAdder();
    private final LongAdder activationLimitSkips = new LongAdder();
    private final LongAdder reentrantSkips = new LongAdder();
    private final TriggerTraceBuffer trace = new TriggerTraceBuffer();

    public int fire(
            ResourceLocation event,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource
    ) {
        return fire(event, owner, eventEntity, damageSource, ItemStack.EMPTY, null);
    }

    public int fire(
            ResourceLocation event,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource,
            ItemStack eventItem,
            BlockState eventBlock
    ) {
        return executeEvent(event, owner, eventEntity, damageSource, eventItem, eventBlock,
                null, 0.0D, null);
    }

    /** Fires an event whose subject is a talent definition, such as a selected draw result. */
    public int fireTalent(
            ResourceLocation event,
            ServerPlayer owner,
            ResourceLocation eventTalent
    ) {
        return executeEvent(event, owner, null, null, ItemStack.EMPTY, null,
                eventTalent, 0.0D, null);
    }

    public int fireMovement(ServerPlayer owner, double horizontalDistance) {
        if (!Double.isFinite(horizontalDistance) || horizontalDistance <= 0.0D) return 0;
        return executeEvent(
                TalentEventTypes.PLAYER_MOVE_DISTANCE, owner, null, null,
                ItemStack.EMPTY, null, null, horizontalDistance, null
        );
    }

    public int fireSubmission(ServerPlayer owner, ItemStack submitted, int count) {
        return executeEvent(TalentEventTypes.ITEM_SUBMITTED, owner, null, null,
                submitted, null, null, count, null);
    }

    public int fireAmount(
            ResourceLocation event,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource,
            double eventAmount
    ) {
        if (!Double.isFinite(eventAmount)) eventAmount = 0.0D;
        return executeEvent(event, owner, eventEntity, damageSource, owner.getMainHandItem(), null,
                null, eventAmount, null);
    }

    public DamageResult fireDamage(
            ResourceLocation event,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource,
            float amount
    ) {
        DamageState damage = new DamageState(amount);
        int applied = executeEvent(
                event, owner, eventEntity, damageSource, owner.getMainHandItem(), null,
                null, amount, damage
        );
        return new DamageResult(applied, damage.amount, damage.canceled);
    }

    private int executeEvent(
            ResourceLocation event,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource,
            ItemStack eventItem,
            BlockState eventBlock,
            ResourceLocation eventTalent,
            double eventAmount,
            DamageState damage
    ) {
        var bindings = TalentEditorMod.SNAPSHOTS.active().triggersByEvent().get(event);
        if (bindings == null || bindings.isEmpty()) return 0;
        eventCalls.increment();
        Set<ResourceLocation> triggerSources = new HashSet<>(PlayerTalents.triggerSources(owner));
        triggerSources.addAll(ConfiguredItem.equippedDefinitionIds(owner));
        if (bindings.stream().noneMatch(binding -> triggerSources.contains(binding.talentId()))) return 0;
        ExecutionKey key = new ExecutionKey(owner.getUUID(), event);
        Set<ExecutionKey> active = activeExecutions.get();
        if (!active.add(key)) {
            reentrantSkips.increment();
            return 0;
        }
        try {
            var context = ConditionEvaluator.eventContext(
                    owner, eventEntity, damageSource, eventItem, eventBlock, eventTalent
            );
            int applied = 0;
            long currentTick = PlayerTalents.counterLifecycleTicks(owner);
            var candidates = TalentTriggerEngine.ownedBindings(bindings, triggerSources);
            for (TriggerBinding binding : candidates) {
                double chance = effectiveChance(owner, binding);
                String cooldownKey = cooldownKey(binding);
                if (event.equals(TalentEventTypes.PLAYER_MOVE_DISTANCE)
                        && !PlayerTalents.claimTriggerDistanceInterval(
                        owner, cooldownKey, eventAmount, binding.trigger().distanceInterval())) {
                    continue;
                }
                if (!ConditionEvaluator.test(
                        binding.trigger().condition(), context,
                        NumericExpressionRuntime.resolver(
                                owner, eventEntity, eventAmount, damage == null ? 0.0D : damage.amount,
                                binding.talentId()
                        )
                )) {
                    trace(owner, event, binding, TriggerTraceEntry.Outcome.CONDITION_MISS, chance, 0);
                    continue;
                }
                matchedTriggers.increment();
                if (binding.trigger().activationLimit() > 0) {
                    int activationCount = PlayerTalents.triggerActivationCount(owner, cooldownKey);
                    if (TalentTriggerEngine.activationLimitReached(
                            activationCount, binding.trigger().activationLimit())
                            || !PlayerTalents.canRecordTriggerActivation(owner, cooldownKey)) {
                        activationLimitSkips.increment();
                        trace(owner, event, binding, TriggerTraceEntry.Outcome.ACTIVATION_LIMIT, chance, 0);
                        continue;
                    }
                }
                long lastActivation = PlayerTalents.triggerLastActivation(owner, cooldownKey);
                if (TalentTriggerEngine.coolingDown(
                        lastActivation, currentTick, binding.trigger().cooldownTicks()
                )) {
                    cooldownSkips.increment();
                    trace(owner, event, binding, TriggerTraceEntry.Outcome.COOLDOWN, chance, 0);
                    continue;
                }
                if (chance <= 0.0D || (chance < 1.0D
                        && !TalentTriggerEngine.chancePasses(chance, owner.getRandom().nextDouble()))) {
                    chanceMisses.increment();
                    trace(owner, event, binding, TriggerTraceEntry.Outcome.CHANCE_MISS, chance, 0);
                    continue;
                }
                ActionChainEngine.Result chain = ActionChainEngine.execute(
                        binding.trigger().actions(), binding.trigger().failureActions(),
                        binding.trigger().actionFailurePolicy(),
                        action -> ActionConditionRuntime.active(action, context,
                                NumericExpressionRuntime.resolver(owner, eventEntity, eventAmount,
                                        damage == null ? 0.0D : damage.amount, binding.talentId())),
                        action -> execute(action, owner, eventEntity, damageSource, eventAmount,
                                binding.talentId(), damage), trace.enabled()
                );
                boolean activated = chain.activated();
                int triggerApplied = chain.appliedActions();
                applied += triggerApplied;
                appliedActions.add(triggerApplied);
                if (activated && binding.trigger().activationLimit() > 0) {
                    PlayerTalents.incrementTriggerActivation(owner, cooldownKey);
                }
                if (activated && binding.trigger().cooldownTicks() > 0) {
                    PlayerTalents.setTriggerLastActivation(owner, cooldownKey, currentTick);
                }
                trace(owner, event, binding, activated
                                ? TriggerTraceEntry.Outcome.ACTIVATED
                                : TriggerTraceEntry.Outcome.NO_ACTION_APPLIED,
                        chance, triggerApplied, chain.attemptedActions(), chain.actionResults());
            }
            return applied;
        } finally {
            active.remove(key);
            if (active.isEmpty()) activeExecutions.remove();
        }
    }

    private static boolean execute(
            CompiledAction action,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource,
            double eventAmount,
            ResourceLocation talentId,
            DamageState damage
    ) {
        RuntimeActionExecutor.DamageAccess access = damage == null ? null : new RuntimeActionExecutor.DamageAccess() {
            @Override public float amount() { return damage.amount; }
            @Override public void amount(float value) { damage.amount = value; }
            @Override public boolean canceled() { return damage.canceled; }
            @Override public void cancel() { damage.canceled = true; }
        };
        return RuntimeActionExecutor.execute(
                com.kidxoxo.talenteditor.domain.ConditionalAction.unwrap(action),
                new RuntimeActionExecutor.Context(
                owner, eventEntity, damageSource, eventAmount, talentId, access
        ));
    }

    private static List<LivingEntity> targets(
            LivingEntityTarget target,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource,
            double radius
    ) {
        if (target == LivingEntityTarget.NEARBY) {
            return owner.serverLevel().getEntitiesOfClass(
                    LivingEntity.class, owner.getBoundingBox().inflate(radius),
                    entity -> entity != owner && entity.isAlive()
            );
        }
        Entity selected = switch (target) {
            case SELF -> owner;
            case EVENT_ENTITY -> eventEntity;
            case DAMAGE_SOURCE -> damageSource == null ? null : damageSource.getEntity();
            case NEARBY -> null;
        };
        if (selected instanceof LivingEntity living && living.isAlive()) return List.of(living);
        return List.of();
    }

    static String cooldownKey(TriggerBinding binding) {
        return binding.talentId() + "#" + binding.trigger().id();
    }

    private static double effectiveChance(ServerPlayer owner, TriggerBinding binding) {
        double chance = binding.trigger().chance();
        if (binding.trigger().chanceAttribute().isPresent()) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(
                    binding.trigger().chanceAttribute().orElseThrow()
            );
            var instance = attribute == null ? null : owner.getAttribute(attribute);
            return instance == null ? chance : TalentTriggerEngine.effectiveChance(
                    chance, instance.getValue(), binding.trigger().chancePerPoint()
            );
        }
        if (binding.trigger().chanceCounter().isPresent()) {
            return TalentTriggerEngine.effectiveChance(
                    chance, PlayerTalents.counter(owner, binding.trigger().chanceCounter().orElseThrow()),
                    binding.trigger().chancePerPoint()
            );
        }
        if (binding.trigger().chanceCategory().isPresent()) {
            ResourceLocation category = binding.trigger().chanceCategory().orElseThrow();
            long count = PlayerTalents.categorySources(owner).stream()
                    .map(TalentEditorMod.SNAPSHOTS.active().talents()::get)
                    .filter(java.util.Objects::nonNull)
                    .filter(talent -> talent.enabled() && talent.available())
                    .filter(talent -> talent.categories().contains(category))
                    .count();
            return TalentTriggerEngine.effectiveChance(
                    chance, count, binding.trigger().chancePerPoint()
            );
        }
        return chance;
    }

    private void trace(
            ServerPlayer owner,
            ResourceLocation event,
            TriggerBinding binding,
            TriggerTraceEntry.Outcome outcome,
            double chance,
            int applied
    ) {
        if (!trace.enabled()) return;
        trace.record(PlayerTalents.counterLifecycleTicks(owner), owner.getGameProfile().getName(), event,
                binding, outcome, chance, applied);
    }

    private void trace(
            ServerPlayer owner,
            ResourceLocation event,
            TriggerBinding binding,
            TriggerTraceEntry.Outcome outcome,
            double chance,
            int applied,
            int attempted,
            List<TriggerTraceEntry.ActionResult> actionResults
    ) {
        if (!trace.enabled()) return;
        trace.record(PlayerTalents.counterLifecycleTicks(owner), owner.getGameProfile().getName(), event,
                binding, outcome, chance, applied, attempted, actionResults);
    }

    public void setTraceEnabled(boolean enabled) {
        trace.setEnabled(enabled);
    }

    public void clearTrace() {
        trace.clear();
    }

    public java.util.List<TriggerTraceEntry> recentTrace(int maximum) {
        return trace.recent(maximum);
    }

    public TriggerTraceBuffer.Snapshot traceStatus() {
        return trace.snapshot();
    }

    public Diagnostics diagnostics() {
        return new Diagnostics(
                eventCalls.sum(), matchedTriggers.sum(), appliedActions.sum(), cooldownSkips.sum(),
                chanceMisses.sum(), activationLimitSkips.sum(), reentrantSkips.sum()
        );
    }

    private record ExecutionKey(UUID player, ResourceLocation event) {
    }

    public record DamageResult(int appliedActions, float amount, boolean canceled) {
    }

    public record Diagnostics(
            long eventCalls,
            long matchedTriggers,
            long appliedActions,
            long cooldownSkips,
            long chanceMisses,
            long activationLimitSkips,
            long reentrantSkips
    ) {
    }

    private static final class DamageState {
        private float amount;
        private boolean canceled;

        private DamageState(float amount) {
            this.amount = Float.isFinite(amount) ? Math.max(0.0F, amount) : 0.0F;
        }
    }
}
