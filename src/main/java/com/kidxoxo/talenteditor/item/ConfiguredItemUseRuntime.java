package com.kidxoxo.talenteditor.item;

import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ConfiguredItemDefinition;
import com.kidxoxo.talenteditor.domain.PlaySoundAction;
import com.kidxoxo.talenteditor.domain.RemoveEffectAction;
import com.kidxoxo.talenteditor.domain.ShowMessageAction;
import com.kidxoxo.talenteditor.runtime.TalentPresentationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

final class ConfiguredItemUseRuntime {
    private static final String COOLDOWNS_TAG = "TalentEditorItemCooldowns";

    private ConfiguredItemUseRuntime() {
    }

    static boolean use(ServerPlayer player, ItemStack stack, ConfiguredItemDefinition definition) {
        var use = definition.use();
        if (!definition.enabled() || !definition.available() || !use.enabled()) return false;
        if (!player.getAbilities().instabuild && stack.getCount() < use.consumeCount()) return false;
        long now = player.level().getGameTime();
        CompoundTag root = player.getPersistentData();
        CompoundTag cooldowns = root.getCompound(COOLDOWNS_TAG);
        if (cooldowns.getLong(definition.id().toString()) > now) return false;

        int applied = 0;
        for (CompiledAction action : use.actions()) {
            if (!com.kidxoxo.talenteditor.runtime.ActionConditionRuntime.active(action, player,
                    definition.id())) continue;
            action = com.kidxoxo.talenteditor.domain.ConditionalAction.unwrap(action);
            if (action instanceof AddEffectAction add && add.target() == ActionTarget.SELF) {
                var effect = ForgeRegistries.MOB_EFFECTS.getValue(add.effect());
                if (effect != null && player.addEffect(new MobEffectInstance(
                        effect, add.durationTicks(), add.amplifier(), add.ambient(),
                        add.showParticles(), add.showIcon()
                ))) applied++;
            } else if (action instanceof RemoveEffectAction remove && remove.target() == ActionTarget.SELF) {
                var effect = ForgeRegistries.MOB_EFFECTS.getValue(remove.effect());
                if (effect != null && player.removeEffect(effect)) applied++;
            } else if (action instanceof PlaySoundAction || action instanceof ShowMessageAction) {
                applied += TalentPresentationService.execute(player, List.of(action));
            }
        }
        if (applied == 0) return false;
        if (use.cooldownTicks() > 0) {
            cooldowns.putLong(definition.id().toString(), now + use.cooldownTicks());
            root.put(COOLDOWNS_TAG, cooldowns);
        }
        if (!player.getAbilities().instabuild && use.consumeCount() > 0) {
            stack.shrink(use.consumeCount());
        }
        return true;
    }
}
