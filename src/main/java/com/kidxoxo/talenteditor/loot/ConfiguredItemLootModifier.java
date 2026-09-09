package com.kidxoxo.talenteditor.loot;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ConfiguredItemLootModifier extends LootModifier {
    public static final Supplier<Codec<ConfiguredItemLootModifier>> CODEC = () -> RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, ConfiguredItemLootModifier::new));

    ConfiguredItemLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null) return generatedLoot;
        var snapshot = TalentEditorMod.SNAPSHOTS.active();
        List<ConfiguredItemLootBinding> exactBindings = snapshot.configuredItemLootByTable()
                .getOrDefault(table, List.of());
        List<ConfiguredItemLootBinding> patternBindings = snapshot.configuredItemLootPatterns();
        if (exactBindings.isEmpty() && patternBindings.isEmpty()) return generatedLoot;
        ArrayList<ConfiguredItemLootBinding> bindings = new ArrayList<>(
                exactBindings.size() + patternBindings.size()
        );
        bindings.addAll(exactBindings);
        patternBindings.stream().filter(binding -> binding.rule().matchesTable(table))
                .forEach(bindings::add);
        for (ConfiguredItemLootSelector.Roll roll : ConfiguredItemLootSelector.roll(
                bindings, runtimeContext(context), context.getRandom()
        )) {
            var definition = snapshot.configuredItems().get(roll.itemId());
            if (definition != null && definition.enabled() && definition.available()) {
                int remaining = roll.count();
                while (remaining > 0) {
                    var stack = ConfiguredItem.create(definition, remaining);
                    remaining -= stack.getCount();
                    generatedLoot.add(stack);
                }
            }
        }
        return generatedLoot;
    }

    private static ConfiguredItemLootContext runtimeContext(LootContext context) {
        return new ConfiguredItemLootContext(
                Optional.of(context.getLevel().dimension().location()),
                entityView(context.getParamOrNull(LootContextParams.THIS_ENTITY)),
                entityView(context.getParamOrNull(LootContextParams.KILLER_ENTITY)),
                context.getLuck()
        );
    }

    private static Optional<ConfiguredItemLootContext.EntityView> entityView(Entity entity) {
        if (entity == null) return Optional.empty();
        ResourceLocation type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (type == null) return Optional.empty();
        Set<ResourceLocation> tags = entity.getType().builtInRegistryHolder().tags()
                .map(tag -> tag.location()).collect(Collectors.toUnmodifiableSet());
        return Optional.of(new ConfiguredItemLootContext.EntityView(type, tags));
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
