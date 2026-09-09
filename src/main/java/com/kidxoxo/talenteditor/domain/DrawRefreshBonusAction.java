package com.kidxoxo.talenteditor.domain;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record DrawRefreshBonusAction(
        int amount,
        List<ResourceLocation> pools
) implements CompiledAction {
    public static final ResourceLocation TYPE = new ResourceLocation(TalentEditorMod.MOD_ID, "draw_refresh_bonus");

    public DrawRefreshBonusAction {
        if (amount < 1 || amount > 100) throw new IllegalArgumentException("amount must be in range 1..100");
        pools = List.copyOf(pools);
    }

    public boolean appliesTo(ResourceLocation pool) {
        return pools.isEmpty() || pools.contains(pool);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }
}
