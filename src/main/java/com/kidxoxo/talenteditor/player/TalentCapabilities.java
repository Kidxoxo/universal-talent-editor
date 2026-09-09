package com.kidxoxo.talenteditor.player;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class TalentCapabilities {
    public static final Capability<PlayerTalentData> PLAYER_TALENTS =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private TalentCapabilities() {
    }
}
