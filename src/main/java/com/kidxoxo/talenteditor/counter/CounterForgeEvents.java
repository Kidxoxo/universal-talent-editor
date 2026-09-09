package com.kidxoxo.talenteditor.counter;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.CounterEventType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CounterForgeEvents {
    private CounterForgeEvents() {
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            TalentEditorMod.COUNTERS.record(
                    CounterEventType.KILL, player, event.getEntity(), player.getMainHandItem(), event.getSource(), 0.0D
            );
        }
    }

    @SubscribeEvent
    public static void livingDamage(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) return;
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            TalentEditorMod.COUNTERS.record(
                    CounterEventType.DAMAGE_DEALT, attacker, event.getEntity(), attacker.getMainHandItem(),
                    event.getSource(), event.getAmount()
            );
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            TalentEditorMod.COUNTERS.record(
                    CounterEventType.DAMAGE_TAKEN, victim, event.getSource().getEntity(), victim.getMainHandItem(),
                    event.getSource(), event.getAmount()
            );
        }
    }

    @SubscribeEvent
    public static void itemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TalentEditorMod.COUNTERS.record(
                    CounterEventType.ITEM_USE, player, null, event.getItem(), null, 0.0D
            );
        }
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getHand() == InteractionHand.MAIN_HAND) {
            TalentEditorMod.COUNTERS.record(
                    CounterEventType.ENTITY_INTERACT, player, event.getTarget(),
                    player.getItemInHand(event.getHand()), null, 0.0D
            );
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            TalentEditorMod.COUNTERS.playerTick(player);
        }
    }
}
