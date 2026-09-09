package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

public final class TalentEffectImmunityService {
    private TalentEffectImmunityService() {
    }

    public static boolean isImmune(ServerPlayer player, ResourceLocation effect) {
        List<ActiveEffectImmunityBinding> bindings = TalentEditorMod.SNAPSHOTS.active()
                .effectImmunityBindings().get(effect);
        if (bindings == null || bindings.isEmpty()) return false;
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        return bindings.stream().anyMatch(binding -> owned.contains(binding.talentId()));
    }
}
