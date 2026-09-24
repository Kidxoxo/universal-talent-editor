package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.TalentLifecycleDefinition;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;

public final class TalentLifecycleService {
    private TalentLifecycleService() {
    }

    public static int process(ServerPlayer player) {
        long tick = PlayerTalents.counterLifecycleTicks(player);
        long draws = PlayerTalents.drawSelections(player);
        ArrayList<TalentDefinition> expired = new ArrayList<>();
        for (ResourceLocation id : PlayerTalents.owned(player)) {
            TalentDefinition talent = TalentEditorMod.SNAPSHOTS.active().talents().get(id);
            if (talent == null || !talent.lifecycle().temporary()) continue;
            TalentLifecycleDefinition lifecycle = talent.lifecycle();
            long acquiredTick = PlayerTalents.acquiredTick(player, id);
            long acquiredDraw = PlayerTalents.acquiredDraw(player, id);
            boolean timeDue = lifecycle.durationTicks() > 0 && acquiredTick >= 0L
                    && tick - acquiredTick >= lifecycle.durationTicks();
            boolean drawDue = lifecycle.removeAfterDraws() > 0 && acquiredDraw >= 0L
                    && draws - acquiredDraw >= lifecycle.removeAfterDraws();
            if (timeDue || drawDue) expired.add(talent);
        }
        if (expired.isEmpty()) return 0;
        int changed = 0;
        for (TalentDefinition talent : expired) {
            TalentLifecycleDefinition lifecycle = talent.lifecycle();
            if (!PlayerTalents.expire(
                    player, talent.id(), lifecycle.retainEffects(), lifecycle.retainCategoryContribution(),
                    lifecycle.retainTriggers()
            )) continue;
            changed++;
            TalentEditorMod.PRESENTATIONS.removed(player, talent);
        }
        if (changed > 0) {
            TalentAttributeRuntime.refresh(player);
            EditorNetwork.sendPlayerState(player, false);
        }
        return changed;
    }
}
