package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TalentTriggerForgeEvents {
    private TalentTriggerForgeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F) return;
        float amount = event.getAmount();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            var result = TalentEditorMod.TRIGGERS.fireDamage(
                    TalentEventTypes.DAMAGE_DEALT_PRE, attacker, event.getEntity(), event.getSource(), amount
            );
            if (result.canceled()) {
                event.setCanceled(true);
                return;
            }
            amount = result.amount();
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            var result = TalentEditorMod.TRIGGERS.fireDamage(
                    TalentEventTypes.DAMAGE_TAKEN_PRE, victim, event.getSource().getEntity(),
                    event.getSource(), amount
            );
            if (result.canceled()) {
                event.setCanceled(true);
                return;
            }
            amount = result.amount();
        }
        event.setAmount(amount);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) return;
        float amount = event.getAmount();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            var result = TalentEditorMod.TRIGGERS.fireDamage(
                    TalentEventTypes.DAMAGE_DEALT, attacker, event.getEntity(), event.getSource(), amount
            );
            if (result.canceled()) {
                event.setCanceled(true);
                return;
            }
            amount = result.amount();
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            if (amount >= victim.getHealth()) {
                var fatal = TalentEditorMod.TRIGGERS.fireDamage(
                        TalentEventTypes.FATAL_DAMAGE, victim, event.getSource().getEntity(),
                        event.getSource(), amount
                );
                if (fatal.canceled()) {
                    event.setCanceled(true);
                    return;
                }
                amount = fatal.amount();
            }
            var result = TalentEditorMod.TRIGGERS.fireDamage(
                    TalentEventTypes.DAMAGE_TAKEN, victim, event.getSource().getEntity(),
                    event.getSource(), amount
            );
            if (result.canceled()) {
                event.setCanceled(true);
                return;
            }
            amount = result.amount();
        }
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getHand() == InteractionHand.MAIN_HAND) {
            TalentEditorMod.TRIGGERS.fire(
                    TalentEventTypes.INTERACT_ENTITY, player, event.getTarget(), null,
                    player.getItemInHand(event.getHand()), null
            );
        }
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            TalentEditorMod.TRIGGERS.fire(
                    TalentEventTypes.KILL, attacker, event.getEntity(), event.getSource(),
                    attacker.getMainHandItem(), null
            );
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            TalentEditorMod.TRIGGERS.fire(
                    TalentEventTypes.DEATH, victim, event.getSource().getEntity(), event.getSource()
            );
        }
        if (!event.getEntity().level().isClientSide() && event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.getEntitiesOfClass(
                    ServerPlayer.class, event.getEntity().getBoundingBox().inflate(64.0D),
                    player -> player.isAlive() && player != event.getEntity()
            ).forEach(player -> TalentEditorMod.TRIGGERS.fire(
                    TalentEventTypes.NEARBY_ENTITY_DEATH, player, event.getEntity(), event.getSource()
            ));
        }
    }

    @SubscribeEvent
    public static void levelChanged(PlayerXpEvent.LevelChange event) {
        if (event.getLevels() > 0 && event.getEntity() instanceof ServerPlayer player) {
            TalentEditorMod.TRIGGERS.fireAmount(
                    TalentEventTypes.LEVEL_UP, player, null, null, event.getLevels()
            );
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) TimedAttributeModifierService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void itemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentEditorMod.TRIGGERS.fire(
                    TalentEventTypes.ITEM_USE_FINISH, player, null, null, event.getItem(), null
            );
        }
    }

    @SubscribeEvent
    public static void blockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getHand() == InteractionHand.MAIN_HAND) {
            TalentEditorMod.TRIGGERS.fire(
                    TalentEventTypes.INTERACT_BLOCK, player, null, null,
                    player.getItemInHand(event.getHand()), player.serverLevel().getBlockState(event.getPos())
            );
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        PlayerMotionTracker.tick(player);
        TalentEditorMod.TRIGGERS.fire(TalentEventTypes.PLAYER_TICK, player, null, null);
        if (player.tickCount % 20 == 0) {
            TalentActiveStatusEffectService.refresh(player);
            TalentLifecycleService.process(player);
            TalentEditorMod.TRIGGERS.fire(TalentEventTypes.PLAYER_SECOND, player, null, null);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var effect = ForgeRegistries.MOB_EFFECTS.getKey(event.getEffectInstance().getEffect());
        if (effect != null && TalentEffectImmunityService.isImmune(player, effect)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
