package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ConditionEvaluator {
    private ConditionEvaluator() {
    }

    public static boolean test(CompiledCondition condition, ServerPlayer player) {
        return test(condition, context(player));
    }

    public static boolean test(CompiledCondition condition, Context context) {
        if (condition instanceof CompiledCondition.Always always) return always.value();
        if (condition instanceof CompiledCondition.All all) {
            return all.children().stream().allMatch(child -> test(child, context));
        }
        if (condition instanceof CompiledCondition.Any any) {
            return any.children().stream().anyMatch(child -> test(child, context));
        }
        if (condition instanceof CompiledCondition.Not not) return !test(not.child(), context);
        if (condition instanceof CompiledCondition.HasTalent hasTalent) {
            return context.talents().contains(hasTalent.talent());
        }
        if (condition instanceof CompiledCondition.CounterAtLeast counter) {
            return context.counters().getOrDefault(counter.counter(), 0L) >= counter.value();
        }
        if (condition instanceof CompiledCondition.CategoryCountAtLeast category) {
            return context.categoryCounts().getOrDefault(category.category(), 0) >= category.count();
        }
        if (condition instanceof CompiledCondition.CounterCategoryTotalAtLeast category) {
            return context.counterCategoryTotals().getOrDefault(category.category(), 0L) >= category.value();
        }
        if (condition instanceof CompiledCondition.HasItem hasItem) {
            return context.itemCounts().getOrDefault(hasItem.item(), 0) >= hasItem.count();
        }
        if (condition instanceof CompiledCondition.EventEntityType entityType) {
            return context.eventEntityType().filter(entityType.entityType()::equals).isPresent();
        }
        if (condition instanceof CompiledCondition.EventEntityTag entityTag) {
            return context.eventEntityTags().contains(entityTag.tag());
        }
        if (condition instanceof CompiledCondition.DamageTypeTag damageTag) {
            return context.damageTypeTags().contains(damageTag.tag());
        }
        if (condition instanceof CompiledCondition.EventItem item) {
            return context.eventItem().filter(item.item()::equals).isPresent();
        }
        if (condition instanceof CompiledCondition.EventItemTag itemTag) {
            return context.eventItemTags().contains(itemTag.tag());
        }
        if (condition instanceof CompiledCondition.EventBlock block) {
            return context.eventBlock().filter(block.block()::equals).isPresent();
        }
        if (condition instanceof CompiledCondition.EventBlockTag blockTag) {
            return context.eventBlockTags().contains(blockTag.tag());
        }
        return false;
    }

    public static Context context(ServerPlayer player) {
        LinkedHashMap<ResourceLocation, Integer> items = new LinkedHashMap<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            Optional<ResourceLocation> logicalId = ConfiguredItem.definitionId(stack);
            if (logicalId.isPresent()) {
                items.merge(logicalId.orElseThrow(), stack.getCount(), Integer::sum);
            } else {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (id != null) items.merge(id, stack.getCount(), Integer::sum);
            }
        }
        Set<ResourceLocation> owned = PlayerTalents.owned(player);
        LinkedHashMap<ResourceLocation, Integer> categoryCounts = new LinkedHashMap<>();
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        for (ResourceLocation id : owned) {
            TalentDefinition talent = snapshot.talents().get(id);
            if (talent == null || !talent.enabled() || !talent.available()) continue;
            talent.categories().forEach(category -> categoryCounts.merge(category, 1, Integer::sum));
        }
        Map<ResourceLocation, Long> counters = PlayerTalents.counters(player);
        LinkedHashMap<ResourceLocation, Long> counterCategoryTotals = new LinkedHashMap<>();
        snapshot.countersByCategory().forEach((category, rules) -> {
            long total = 0L;
            for (var rule : rules) {
                long value = counters.getOrDefault(rule.id(), 0L);
                if (value >= com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE - total) {
                    total = com.kidxoxo.talenteditor.player.PlayerTalentData.MAX_COUNTER_VALUE;
                    break;
                }
                total += value;
            }
            if (total > 0L) counterCategoryTotals.put(category, total);
        });
        return new Context(owned, counters, items, categoryCounts, counterCategoryTotals);
    }

    public static Context eventContext(ServerPlayer player, Entity eventEntity, DamageSource damageSource) {
        return eventContext(player, eventEntity, damageSource, ItemStack.EMPTY, null);
    }

    public static Context eventContext(
            ServerPlayer player,
            Entity eventEntity,
            DamageSource damageSource,
            ItemStack eventItem,
            BlockState eventBlock
    ) {
        Context base = context(player);
        ResourceLocation type = eventEntity == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(eventEntity.getType());
        Set<ResourceLocation> entityTags = eventEntity == null ? Set.of() : eventEntity.getType()
                .builtInRegistryHolder().tags().map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        Set<ResourceLocation> damageTags = damageSource == null ? Set.of() : damageSource.typeHolder().tags()
                .map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        ItemStack effectiveItem = eventItem == null ? ItemStack.EMPTY : eventItem;
        ResourceLocation item = effectiveItem.isEmpty() ? null : ConfiguredItem.definitionId(effectiveItem)
                .orElseGet(() -> ForgeRegistries.ITEMS.getKey(effectiveItem.getItem()));
        Set<ResourceLocation> itemTags = effectiveItem.isEmpty() ? Set.of() : effectiveItem.getItem()
                .builtInRegistryHolder().tags().map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        ResourceLocation block = eventBlock == null ? null : ForgeRegistries.BLOCKS.getKey(eventBlock.getBlock());
        Set<ResourceLocation> blockTags = eventBlock == null ? Set.of() : eventBlock.getBlock()
                .builtInRegistryHolder().tags().map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        return new Context(
                base.talents(), base.counters(), base.itemCounts(), base.categoryCounts(),
                base.counterCategoryTotals(), Optional.ofNullable(type), entityTags, damageTags,
                Optional.ofNullable(item), itemTags, Optional.ofNullable(block), blockTags
        );
    }

    public record Context(
            Set<ResourceLocation> talents,
            Map<ResourceLocation, Long> counters,
            Map<ResourceLocation, Integer> itemCounts,
            Map<ResourceLocation, Integer> categoryCounts,
            Map<ResourceLocation, Long> counterCategoryTotals,
            Optional<ResourceLocation> eventEntityType,
            Set<ResourceLocation> eventEntityTags,
            Set<ResourceLocation> damageTypeTags,
            Optional<ResourceLocation> eventItem,
            Set<ResourceLocation> eventItemTags,
            Optional<ResourceLocation> eventBlock,
            Set<ResourceLocation> eventBlockTags
    ) {
        public Context {
            talents = Set.copyOf(talents);
            counters = Map.copyOf(counters);
            itemCounts = Map.copyOf(itemCounts);
            categoryCounts = Map.copyOf(categoryCounts);
            counterCategoryTotals = Map.copyOf(counterCategoryTotals);
            eventEntityType = eventEntityType == null ? Optional.empty() : eventEntityType;
            eventEntityTags = Set.copyOf(eventEntityTags);
            damageTypeTags = Set.copyOf(damageTypeTags);
            eventItem = eventItem == null ? Optional.empty() : eventItem;
            eventItemTags = Set.copyOf(eventItemTags);
            eventBlock = eventBlock == null ? Optional.empty() : eventBlock;
            eventBlockTags = Set.copyOf(eventBlockTags);
        }

        public Context(
                Set<ResourceLocation> talents,
                Map<ResourceLocation, Long> counters,
                Map<ResourceLocation, Integer> itemCounts,
                Map<ResourceLocation, Integer> categoryCounts,
                Map<ResourceLocation, Long> counterCategoryTotals,
                Optional<ResourceLocation> eventEntityType,
                Set<ResourceLocation> eventEntityTags,
                Set<ResourceLocation> damageTypeTags
        ) {
            this(talents, counters, itemCounts, categoryCounts, counterCategoryTotals,
                    eventEntityType, eventEntityTags, damageTypeTags,
                    Optional.empty(), Set.of(), Optional.empty(), Set.of());
        }

        public Context(
                Set<ResourceLocation> talents,
                Map<ResourceLocation, Long> counters,
                Map<ResourceLocation, Integer> itemCounts,
                Map<ResourceLocation, Integer> categoryCounts,
                Map<ResourceLocation, Long> counterCategoryTotals
        ) {
            this(talents, counters, itemCounts, categoryCounts, counterCategoryTotals,
                    Optional.empty(), Set.of(), Set.of(), Optional.empty(), Set.of(), Optional.empty(), Set.of());
        }

        public Context(
                Set<ResourceLocation> talents,
                Map<ResourceLocation, Long> counters,
                Map<ResourceLocation, Integer> itemCounts,
                Map<ResourceLocation, Integer> categoryCounts
        ) {
            this(talents, counters, itemCounts, categoryCounts, Map.of());
        }

        public Context(
                Set<ResourceLocation> talents,
                Map<ResourceLocation, Long> counters,
                Map<ResourceLocation, Integer> itemCounts
        ) {
            this(talents, counters, itemCounts, Map.of(), Map.of());
        }

        public static Context empty(Set<ResourceLocation> talents) {
            return new Context(talents, Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
