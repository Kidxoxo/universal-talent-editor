package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.player.PlayerTalentData;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class EntityNumericStateService {
    private static final String ROOT = TalentEditorMod.MOD_ID + ":numeric_states";

    private EntityNumericStateService() {
    }

    public static double get(LivingEntity entity, ResourceLocation id) {
        if (entity instanceof ServerPlayer player) return PlayerTalents.numericState(player, id);
        CompoundTag values = entity.getPersistentData().getCompound(ROOT);
        double value = values.getDouble(id.toString());
        return Double.isFinite(value) ? value : 0.0D;
    }

    public static double set(LivingEntity entity, ResourceLocation id, double value) {
        if (entity instanceof ServerPlayer player) return PlayerTalents.setNumericState(player, id, value);
        double bounded = Double.isFinite(value)
                ? Math.max(-PlayerTalentData.MAX_NUMERIC_STATE_VALUE,
                Math.min(PlayerTalentData.MAX_NUMERIC_STATE_VALUE, value)) : 0.0D;
        CompoundTag persistent = entity.getPersistentData();
        CompoundTag values = persistent.getCompound(ROOT);
        if (!values.contains(id.toString()) && values.getAllKeys().size() >= PlayerTalentData.MAX_NUMERIC_STATES) {
            return get(entity, id);
        }
        if (Math.abs(bounded) < 1.0E-12D) values.remove(id.toString());
        else values.putDouble(id.toString(), bounded);
        if (values.isEmpty()) persistent.remove(ROOT); else persistent.put(ROOT, values);
        return bounded;
    }
}
