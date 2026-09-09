package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.Set;

public final class TalentActiveStatusEffectService {
    private static final int MINIMUM_LEASE_TICKS = 40;
    private static final int MAXIMUM_LEASE_TICKS = 200;
    private static final int REFRESH_AT_TICKS = 30;

    private TalentActiveStatusEffectService() {
    }

    public static void refresh(ServerPlayer player) {
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        if (owned.isEmpty()) return;
        TalentEditorMod.SNAPSHOTS.active().activeStatusEffectBindings().forEach((effectId, bindings) ->
                bindings.stream()
                        .filter(binding -> owned.contains(binding.talentId()))
                        .max(Comparator.comparingInt(binding -> binding.action().amplifier()))
                        .ifPresent(binding -> apply(player, effectId, binding.action()))
        );
    }

    static int leaseTicks(AddEffectAction action) {
        return Math.max(MINIMUM_LEASE_TICKS, Math.min(MAXIMUM_LEASE_TICKS, action.durationTicks()));
    }

    private static void apply(ServerPlayer player, ResourceLocation effectId, AddEffectAction action) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) return;
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && (current.getAmplifier() > action.amplifier()
                || current.getAmplifier() == action.amplifier() && current.getDuration() > REFRESH_AT_TICKS)) return;
        player.addEffect(new MobEffectInstance(
                effect, leaseTicks(action), action.amplifier(),
                action.ambient(), action.showParticles(), action.showIcon()
        ));
    }
}
