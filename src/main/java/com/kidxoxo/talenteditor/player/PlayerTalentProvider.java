package com.kidxoxo.talenteditor.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerTalentProvider implements ICapabilitySerializable<CompoundTag> {
    private final PlayerTalentData data = new PlayerTalentData();
    private LazyOptional<PlayerTalentData> optional = LazyOptional.of(() -> data);
    private boolean invalidated;

    @Override
    public synchronized <T> LazyOptional<T> getCapability(
            @Nonnull Capability<T> capability,
            @Nullable Direction side
    ) {
        if (capability != TalentCapabilities.PLAYER_TALENTS) return LazyOptional.empty();
        return talentData().cast();
    }

    synchronized LazyOptional<PlayerTalentData> talentData() {
        /*
         * Forge revives the entity-level CapabilityProvider during player cloning and dimension
         * transport, but it does not notify attached providers to recreate LazyOptional instances
         * invalidated by AttachCapabilitiesEvent listeners. The entity validity gate prevents this
         * method being reached while genuinely removed; reaching it again means Forge revived the
         * player and the same data object must become queryable without being cleared.
         */
        if (invalidated) {
            optional = LazyOptional.of(() -> data);
            invalidated = false;
        }
        return optional;
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }

    public synchronized void invalidate() {
        if (invalidated) return;
        optional.invalidate();
        invalidated = true;
    }
}
