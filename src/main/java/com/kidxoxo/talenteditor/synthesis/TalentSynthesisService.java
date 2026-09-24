package com.kidxoxo.talenteditor.synthesis;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.SynthesisDefinition;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.kidxoxo.talenteditor.player.PlayerTalentData;
import com.kidxoxo.talenteditor.player.TalentCapabilities;
import com.kidxoxo.talenteditor.runtime.ConditionEvaluator;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TalentSynthesisService {
    /**
     * Resolves all recipes that are currently ready, including recipes unlocked by an earlier result.
     * A stable ID order makes competing recipes deterministic; the bound prevents malformed packs from
     * monopolizing a server tick.
     */
    public synchronized int synthesizeReady(ServerPlayer player, int maximum) {
        int limit = Math.max(0, Math.min(256, maximum));
        int synthesized = 0;
        while (synthesized < limit) {
            ResourceLocation ready = TalentEditorMod.SNAPSHOTS.active().talents().values().stream()
                    .filter(TalentDefinition::enabled)
                    .filter(TalentDefinition::available)
                    .filter(talent -> talent.synthesis().enabled())
                    .sorted(java.util.Comparator.comparing(talent -> talent.id().toString()))
                    .map(TalentDefinition::id)
                    .filter(id -> check(player, id).successful())
                    .findFirst()
                    .orElse(null);
            if (ready == null || !synthesize(player, ready).successful()) break;
            synthesized++;
        }
        return synthesized;
    }

    public SynthesisResult check(ServerPlayer player, ResourceLocation targetId) {
        TalentDefinition target = TalentEditorMod.SNAPSHOTS.active().talents().get(targetId);
        if (target == null) return SynthesisResult.failed(SynthesisResult.Status.TARGET_NOT_FOUND, targetId);
        if (!target.enabled() || !target.available()) {
            return SynthesisResult.failed(SynthesisResult.Status.TARGET_UNAVAILABLE, targetId);
        }
        SynthesisDefinition synthesis = target.synthesis();
        if (!synthesis.enabled()) {
            return SynthesisResult.failed(SynthesisResult.Status.NOT_SYNTHESIZABLE, targetId);
        }
        PlayerTalentData data = player.getCapability(TalentCapabilities.PLAYER_TALENTS).resolve().orElse(null);
        if (data == null) {
            return SynthesisResult.failed(SynthesisResult.Status.PLAYER_DATA_UNAVAILABLE, targetId);
        }
        if (data.contains(targetId)) {
            return SynthesisResult.failed(SynthesisResult.Status.ALREADY_OWNED, targetId);
        }
        if (!ConditionEvaluator.test(target.acquireCondition(), player)) {
            return SynthesisResult.failed(SynthesisResult.Status.REQUIREMENTS_NOT_MET, targetId);
        }
        for (ResourceLocation talent : synthesis.consumeTalents()) {
            if (!data.contains(talent)) {
                return SynthesisResult.failed(SynthesisResult.Status.MISSING_TALENT, targetId, talent);
            }
        }
        for (Map.Entry<ResourceLocation, Long> cost : synthesis.consumeCounters().entrySet()) {
            if (data.counter(cost.getKey()) < cost.getValue()) {
                return SynthesisResult.failed(SynthesisResult.Status.MISSING_COUNTER, targetId, cost.getKey());
            }
        }
        Map<ResourceLocation, Integer> itemCounts = ConditionEvaluator.context(player).itemCounts();
        for (Map.Entry<ResourceLocation, Integer> cost : synthesis.consumeItems().entrySet()) {
            if (itemCounts.getOrDefault(cost.getKey(), 0) < cost.getValue()) {
                return SynthesisResult.failed(SynthesisResult.Status.MISSING_ITEM, targetId, cost.getKey());
            }
        }
        return SynthesisResult.success(targetId);
    }

    public synchronized SynthesisResult synthesize(ServerPlayer player, ResourceLocation targetId) {
        SynthesisResult checked = check(player, targetId);
        if (!checked.successful()) return checked;
        TalentDefinition target = TalentEditorMod.SNAPSHOTS.active().talents().get(targetId);
        if (target == null) return SynthesisResult.failed(SynthesisResult.Status.TARGET_NOT_FOUND, targetId);
        PlayerTalentData data = player.getCapability(TalentCapabilities.PLAYER_TALENTS).resolve().orElse(null);
        if (data == null) {
            return SynthesisResult.failed(SynthesisResult.Status.PLAYER_DATA_UNAVAILABLE, targetId);
        }
        SynthesisDefinition synthesis = target.synthesis();
        List<SlotConsumption> itemPlan = planItems(player.getInventory(), synthesis.consumeItems());
        if (itemPlan == null) {
            return SynthesisResult.failed(SynthesisResult.Status.MISSING_ITEM, targetId);
        }
        Set<ResourceLocation> consumedTalents = new LinkedHashSet<>(synthesis.consumeTalents());
        if (!data.applySynthesis(targetId, consumedTalents, synthesis.consumeCounters())) {
            return SynthesisResult.failed(SynthesisResult.Status.PLAYER_DATA_UNAVAILABLE, targetId);
        }
        consumedTalents.stream()
                .map(TalentEditorMod.SNAPSHOTS.active().talents()::get)
                .filter(java.util.Objects::nonNull)
                .forEach(consumed -> TalentEditorMod.PRESENTATIONS.removed(player, consumed));
        itemPlan.forEach(consumption ->
                player.getInventory().getItem(consumption.slot()).shrink(consumption.count())
        );
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        TalentEditorMod.PRESENTATIONS.acquired(player, target, true);
        TalentAttributeRuntime.refresh(player);
        EditorNetwork.sendPlayerState(player, false);
        return SynthesisResult.success(targetId);
    }

    private static List<SlotConsumption> planItems(
            Inventory inventory,
            Map<ResourceLocation, Integer> costs
    ) {
        ArrayList<SlotConsumption> plan = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> cost : costs.entrySet()) {
            int remaining = cost.getValue();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack.isEmpty() || !matchesItem(stack, cost.getKey())) continue;
                int taken = Math.min(remaining, stack.getCount());
                plan.add(new SlotConsumption(slot, taken));
                remaining -= taken;
            }
            if (remaining > 0) return null;
        }
        return List.copyOf(plan);
    }

    private static boolean matchesItem(ItemStack stack, ResourceLocation id) {
        return ConfiguredItem.definitionId(stack)
                .map(id::equals)
                .orElseGet(() -> id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem())));
    }

    private record SlotConsumption(int slot, int count) {
    }
}
