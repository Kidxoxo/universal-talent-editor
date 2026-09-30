package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.*;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import com.kidxoxo.talenteditor.player.PlayerTalentData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

public final class RuntimeActionExecutor {
    private RuntimeActionExecutor() {
    }

    public static boolean execute(CompiledAction action, Context context) {
        ServerPlayer owner = context.owner();
        if (!ActionConditionRuntime.active(action,
                ConditionEvaluator.eventContext(owner, context.eventEntity(), context.damageSource()),
                NumericExpressionRuntime.resolver(owner, context.eventEntity(), context.eventAmount(),
                        context.currentDamage(), context.talentId()))) return false;
        action = ConditionalAction.unwrap(action);
        if (action instanceof AddEffectAction addEffect) {
            Entity target = addEffect.target() == ActionTarget.SELF ? owner : context.eventEntity();
            if (!(target instanceof LivingEntity living)) return false;
            var effect = ForgeRegistries.MOB_EFFECTS.getValue(addEffect.effect());
            return effect != null && living.addEffect(new MobEffectInstance(
                    effect, addEffect.durationTicks(), addEffect.amplifier(), addEffect.ambient(),
                    addEffect.showParticles(), addEffect.showIcon()
            ));
        }
        if (action instanceof RemoveEffectAction removeEffect) {
            Entity target = removeEffect.target() == ActionTarget.SELF ? owner : context.eventEntity();
            if (!(target instanceof LivingEntity living)) return false;
            var effect = ForgeRegistries.MOB_EFFECTS.getValue(removeEffect.effect());
            return effect != null && living.removeEffect(effect);
        }
        if (action instanceof ModifyDamageAction modifyDamage) {
            if (context.damage() == null) return false;
            float previous = context.damage().amount();
            context.damage().amount(modifyDamage.apply(previous));
            return Float.compare(previous, context.damage().amount()) != 0;
        }
        if (action instanceof ModifyDamageExpressionAction modifyDamage) {
            if (context.damage() == null) return false;
            float previous = context.damage().amount();
            double evaluated = NumericExpressionRuntime.evaluate(
                    modifyDamage.expression(), owner, context.eventEntity(), context.eventAmount(),
                    previous, context.talentId()
            );
            context.damage().amount(modifyDamage.apply(previous, evaluated));
            return Float.compare(previous, context.damage().amount()) != 0;
        }
        if (action instanceof CancelDamageAction) {
            if (context.damage() == null || context.damage().canceled()) return false;
            context.damage().amount(0.0F);
            context.damage().cancel();
            return true;
        }
        if (action instanceof ModifyEntityAttributeAction modifyAttribute) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(modifyAttribute.attribute());
            if (attribute == null) return false;
            boolean changed = false;
            EntitySelectorSpec selector = legacySelector(
                    modifyAttribute.target(), modifyAttribute.radius()
            );
            for (LivingEntity living : EntityTargetSelector.select(
                    selector, owner, context.eventEntity(), context.damageSource()
            )) {
                var instance = living.getAttribute(attribute);
                if (instance == null) continue;
                double previous = instance.getBaseValue();
                double next = modifyAttribute.apply(previous);
                if (Double.compare(previous, next) != 0) {
                    instance.setBaseValue(next);
                    changed = true;
                }
            }
            return changed;
        }
        if (action instanceof ModifyAttributeBaseExpressionAction modifyAttribute) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(modifyAttribute.attribute());
            if (attribute == null) return false;
            boolean changed = false;
            for (LivingEntity living : EntityTargetSelector.select(
                    modifyAttribute.selector(), owner, context.eventEntity(), context.damageSource()
            )) {
                var instance = living.getAttribute(attribute);
                if (instance == null) continue;
                double previous = instance.getBaseValue();
                double amount = NumericExpressionRuntime.evaluate(
                        modifyAttribute.expression(), owner, living, context.eventAmount(),
                        context.currentDamage(), context.talentId()
                );
                double next = Math.max(modifyAttribute.minimum(), Math.min(
                        modifyAttribute.maximum(), modifyAttribute.operation().apply(previous, amount)
                ));
                if (Double.compare(previous, next) != 0) {
                    instance.setBaseValue(next);
                    changed = true;
                }
            }
            return changed;
        }
        if (action instanceof TimedAttributeModifierAction timed) {
            boolean changed = false;
            for (LivingEntity living : EntityTargetSelector.select(
                    timed.selector(), owner, context.eventEntity(), context.damageSource()
            )) {
                double amount = NumericExpressionRuntime.evaluate(
                        timed.expression(), owner, living, context.eventAmount(),
                        context.currentDamage(), context.talentId()
                );
                changed |= TimedAttributeModifierService.apply(
                        living, timed.attribute(), amount, timed.operation(), timed.modifierKey(),
                        owner.getUUID(), context.talentId(), timed.durationTicks()
                );
            }
            return changed;
        }
        if (action instanceof DamageEntityAction damageEntity) {
            boolean changed = false;
            for (LivingEntity living : EntityTargetSelector.select(
                    legacySelector(damageEntity.target(), damageEntity.radius()), owner,
                    context.eventEntity(), context.damageSource()
            )) {
                changed |= living.hurt(
                        living == owner ? owner.damageSources().magic() : owner.damageSources().thorns(owner),
                        damageEntity.amount()
                );
            }
            return changed;
        }
        if (action instanceof HealEntityAction healEntity) {
            boolean changed = false;
            for (LivingEntity living : EntityTargetSelector.select(
                    legacySelector(healEntity.target(), healEntity.radius()), owner,
                    context.eventEntity(), context.damageSource()
            )) {
                float previous = living.getHealth();
                living.heal(healEntity.amount());
                changed |= Float.compare(previous, living.getHealth()) != 0;
            }
            return changed;
        }
        if (action instanceof AffectHealthExpressionAction affectHealth) {
            boolean changed = false;
            for (LivingEntity living : EntityTargetSelector.select(
                    affectHealth.selector(), owner, context.eventEntity(), context.damageSource()
            )) {
                double evaluated = NumericExpressionRuntime.evaluate(
                        affectHealth.expression(), owner, living, context.eventAmount(),
                        context.currentDamage(), context.talentId()
                );
                HealthOperation operation = affectHealth.resolvedOperation(evaluated);
                float amount = (float) affectHealth.boundedMagnitude(evaluated);
                if (amount <= 0.0F) continue;
                if (operation == HealthOperation.DAMAGE) {
                    changed |= living.hurt(
                            living == owner ? owner.damageSources().magic() : owner.damageSources().thorns(owner),
                            amount
                    );
                } else {
                    float previous = living.getHealth();
                    living.heal(amount);
                    changed |= Float.compare(previous, living.getHealth()) != 0;
                }
            }
            return changed;
        }
        if (action instanceof ModifyNumericStateAction modifyState) {
            boolean changed = false;
            for (LivingEntity living : EntityTargetSelector.select(
                    modifyState.selector(), owner, context.eventEntity(), context.damageSource()
            )) {
                double previous = EntityNumericStateService.get(living, modifyState.state());
                double amount = NumericExpressionRuntime.evaluate(
                        modifyState.expression(), owner, living, context.eventAmount(),
                        context.currentDamage(), context.talentId()
                );
                double next = Math.max(modifyState.minimum(), Math.min(
                        modifyState.maximum(), modifyState.operation().apply(previous, amount)
                ));
                EntityNumericStateService.set(living, modifyState.state(), next);
                boolean stateChanged = Double.compare(previous, next) != 0;
                changed |= stateChanged;
                if (stateChanged && living instanceof ServerPlayer changedPlayer) {
                    EditorNetwork.sendHudResources(changedPlayer);
                }
            }
            return changed;
        }
        if (action instanceof QueueDrawConstraintAction constraint) {
            boolean queued = PlayerTalents.queueDrawConstraint(owner, constraint);
            if (queued) {
                constraint.successCounter().ifPresent(counter ->
                        TalentEditorMod.COUNTERS.markExternalChange(owner, counter));
                if (constraint.successCounter().isPresent()) {
                    TalentDefinition source = TalentEditorMod.SNAPSHOTS.active().talents().get(context.talentId());
                    owner.sendSystemMessage(Component.translatable(
                            "message.talent_editor.search_success",
                            source == null ? Component.translatable("message.talent_editor.unknown_talent")
                                    : source.name().component()
                    ));
                }
            }
            return queued;
        }
        if (action instanceof GrantTalentAction grant) return grantTalent(owner, grant);
        if (action instanceof RevokeTalentAction revoke) return revokeTalent(owner, revoke);
        if (action instanceof GiveItemAction giveItem) return giveItem(owner, giveItem, context);
        if (action instanceof GiveDrawTokensAction giveTokens) {
            int amount = boundedCount(NumericExpressionRuntime.evaluate(
                    giveTokens.amount(), owner, context.eventEntity(), context.eventAmount(),
                    context.currentDamage(), context.talentId()
            ), PlayerTalentData.MAX_DRAW_TOKENS);
            if (amount <= 0) return false;
            int previous = PlayerTalents.drawTokens(owner);
            int next = PlayerTalents.addDrawTokens(owner, amount);
            if (next != previous) EditorNetwork.sendPlayerState(owner, false);
            return next != previous;
        }
        if (action instanceof ModifyCounterAction modifyCounter) {
            long previous = PlayerTalents.counter(owner, modifyCounter.counter());
            double amount = NumericExpressionRuntime.evaluate(
                    modifyCounter.expression(), owner, context.eventEntity(), context.eventAmount(),
                    context.currentDamage(), context.talentId()
            );
            double raw = modifyCounter.operation().apply(previous, amount);
            long next = Double.isFinite(raw) ? Math.round(Math.max(
                    modifyCounter.minimum(), Math.min(modifyCounter.maximum(), raw)
            )) : previous;
            if (next == previous) return false;
            PlayerTalents.setCounter(owner, modifyCounter.counter(), next);
            TalentEditorMod.COUNTERS.markExternalChange(owner, modifyCounter.counter());
            return true;
        }
        if (action instanceof TakeItemAction takeItem) return takeItem(owner, takeItem, context);
        if (action instanceof SpawnParticlesAction particles) return ParticleAnimationService.start(owner, particles);
        if (action instanceof PresentationTransformAction transform) return PresentationTransformService.start(owner, transform);
        if (action instanceof OpenItemSubmissionAction submission) {
            int count = boundedCount(NumericExpressionRuntime.evaluate(
                    submission.count(), owner, context.eventEntity(), context.eventAmount(),
                    context.currentDamage(), context.talentId()
            ), 4_096);
            return count > 0 && TalentEditorMod.SUBMISSIONS.open(
                    owner, submission.item(), count, submission.timeoutTicks(), submission.consume()
            );
        }
        if (action instanceof PlaySoundAction || action instanceof ShowMessageAction) {
            return TalentPresentationService.executePresentation(owner, action, false);
        }
        TalentEditorMod.LOGGER.warn("Ignored unsupported runtime action {}", action.type());
        return false;
    }

    public static int executeLifecycle(
            ServerPlayer player, ResourceLocation talent, List<CompiledAction> actions, boolean synthesized
    ) {
        int applied = 0;
        Context context = new Context(player, null, null, 0.0D, talent, null);
        for (CompiledAction action : actions) {
            if (!ActionConditionRuntime.active(action, player, talent)) continue;
            action = ConditionalAction.unwrap(action);
            RandomObservation random = RandomObservation.capture(player, action);
            boolean changed = PresentationTimelineService.atTick(action) > 0
                    ? PresentationTimelineService.schedule(player, action, synthesized)
                    : action instanceof PlaySoundAction || action instanceof ShowMessageAction
                    ? TalentPresentationService.executePresentation(player, action, synthesized)
                    : execute(action, context);
            if (changed) {
                applied++;
                random.report(player, talent, action);
            }
        }
        return applied;
    }

    private static boolean grantTalent(ServerPlayer player, GrantTalentAction action) {
        TalentDefinition talent = TalentEditorMod.SNAPSHOTS.active().talents().get(action.talent());
        if (talent == null || !talent.enabled() || !talent.available()) return false;
        if (action.onlyIfNeverAcquired() && PlayerTalents.hasEverAcquired(player, action.talent())) return false;
        if (!ConditionEvaluator.test(talent.acquireCondition(), player)) return false;
        if (!PlayerTalents.grant(player, action.talent())) return false;
        TalentAttributeRuntime.refresh(player);
        EditorNetwork.sendPlayerState(player, false);
        TalentEditorMod.PRESENTATIONS.acquired(player, talent, action.synthesized());
        return true;
    }

    private static boolean revokeTalent(ServerPlayer player, RevokeTalentAction action) {
        TalentDefinition talent = TalentEditorMod.SNAPSHOTS.active().talents().get(action.talent());
        if (!PlayerTalents.expire(
                player, action.talent(), action.retainEffects(), action.retainCategoryContribution(),
                action.retainTriggers()
        )) return false;
        if (talent != null) TalentEditorMod.PRESENTATIONS.removed(player, talent);
        TalentAttributeRuntime.refresh(player);
        EditorNetwork.sendPlayerState(player, false);
        return true;
    }

    private static boolean giveItem(ServerPlayer player, GiveItemAction action, Context context) {
        int count = boundedCount(NumericExpressionRuntime.evaluate(
                action.count(), player, context.eventEntity(), context.eventAmount(),
                context.currentDamage(), context.talentId()
        ), 4_096);
        if (count <= 0) return false;
        ConfiguredItemDefinition configured = TalentEditorMod.SNAPSHOTS.active().configuredItems().get(action.item());
        Item vanilla = configured == null ? ForgeRegistries.ITEMS.getValue(action.item()) : null;
        if (configured == null && vanilla == null) return false;
        boolean changed = false;
        int remaining = count;
        int maximum = configured == null ? vanilla.getMaxStackSize() : 64;
        while (remaining > 0) {
            int batch = Math.min(remaining, maximum);
            ItemStack stack = configured == null ? new ItemStack(vanilla, batch) : ConfiguredItem.create(configured, batch);
            remaining -= batch;
            if (!player.addItem(stack) && !stack.isEmpty()) {
                if (action.dropOverflow()) player.drop(stack, false);
                else break;
            }
            changed = true;
        }
        return changed;
    }

    private static boolean takeItem(ServerPlayer player, TakeItemAction action, Context context) {
        int count = boundedCount(NumericExpressionRuntime.evaluate(
                action.count(), player, context.eventEntity(), context.eventAmount(),
                context.currentDamage(), context.talentId()
        ), 4_096);
        if (count <= 0) return false;
        int available = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (matches(stack, action.item())) available += stack.getCount();
        }
        if (available < count) return false;
        int remaining = count;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!matches(stack, action.item())) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    private static boolean matches(ItemStack stack, ResourceLocation item) {
        if (stack.isEmpty()) return false;
        return ConfiguredItem.definitionId(stack).map(item::equals)
                .orElseGet(() -> item.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())));
    }

    private static int boundedCount(double value, int maximum) {
        if (!Double.isFinite(value)) return 0;
        return (int) Math.max(0L, Math.min(maximum, Math.round(value)));
    }

    private static EntitySelectorSpec legacySelector(LivingEntityTarget target, double radius) {
        return new EntitySelectorSpec(target, radius, EntityRelation.ANY, EntityOrder.NEAREST, 256);
    }

    private record RandomObservation(double value, boolean eligible) {
        private static RandomObservation capture(ServerPlayer player, CompiledAction action) {
            if (action instanceof ModifyAttributeBaseExpressionAction attribute
                    && attribute.expression().source().contains("random()")) {
                Attribute type = ForgeRegistries.ATTRIBUTES.getValue(attribute.attribute());
                var instance = type == null ? null : player.getAttribute(type);
                return new RandomObservation(instance == null ? 0.0D : instance.getBaseValue(), instance != null);
            }
            if (action instanceof ModifyNumericStateAction state
                    && state.expression().source().contains("random()")
                    && !state.state().getPath().contains("friend_roll")) {
                return new RandomObservation(PlayerTalents.numericState(player, state.state()), true);
            }
            if (action instanceof GiveDrawTokensAction tokens && tokens.amount().source().contains("random()")) {
                return new RandomObservation(PlayerTalents.drawTokens(player), true);
            }
            return new RandomObservation(0.0D, false);
        }

        private void report(ServerPlayer player, ResourceLocation talentId, CompiledAction action) {
            if (!eligible) return;
            TalentDefinition talent = TalentEditorMod.SNAPSHOTS.active().talents().get(talentId);
            Component talentName = talent == null
                    ? Component.translatable("message.talent_editor.unknown_talent") : talent.name().component();
            Component effect;
            String result;
            if (action instanceof ModifyAttributeBaseExpressionAction attribute) {
                Attribute type = ForgeRegistries.ATTRIBUTES.getValue(attribute.attribute());
                var instance = type == null ? null : player.getAttribute(type);
                if (instance == null) return;
                double next = instance.getBaseValue();
                effect = Component.translatable(type.getDescriptionId());
                result = switch (attribute.operation()) {
                    case ADD -> signed(next - value);
                    case MULTIPLY -> value == 0.0D ? compact(next)
                            : signed((next / value - 1.0D) * 100.0D) + "%";
                    case SET -> compact(next);
                };
            } else if (action instanceof ModifyNumericStateAction state) {
                double next = PlayerTalents.numericState(player, state.state());
                String path = state.state().getPath();
                if (path.contains("armor_roll")) {
                    effect = Component.translatable("attribute.name.generic.armor");
                    result = signed(next);
                } else if (path.contains("damage_roll")) {
                    effect = Component.translatable("message.talent_editor.random.final_damage");
                    result = signed(next * 100.0D) + "%";
                } else if (path.contains("shared_roll")) {
                    effect = Component.translatable("message.talent_editor.random.health_attack");
                    result = signed(next * 100.0D) + "%";
                } else {
                    effect = Component.translatable("message.talent_editor.random.value");
                    result = compact(next);
                }
            } else if (action instanceof GiveDrawTokensAction) {
                effect = Component.translatable("message.talent_editor.random.draw_tokens");
                result = signed(PlayerTalents.drawTokens(player) - value);
            } else return;
            player.sendSystemMessage(Component.translatable(
                    "message.talent_editor.random_result", talentName, effect, result
            ));
        }

        private static String signed(double value) {
            String formatted = compact(value);
            return value >= 0.0D ? "+" + formatted : formatted;
        }

        private static String compact(double value) {
            if (value == Math.rint(value)) return Long.toString((long) value);
            return String.format(Locale.ROOT, "%.2f", value)
                    .replaceFirst("0+$", "").replaceFirst("\\.$", "");
        }
    }

    public record Context(
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource,
            double eventAmount,
            ResourceLocation talentId,
            DamageAccess damage
    ) {
        public double currentDamage() {
            return damage == null ? 0.0D : damage.amount();
        }
    }

    public interface DamageAccess {
        float amount();
        void amount(float value);
        boolean canceled();
        void cancel();
    }
}
