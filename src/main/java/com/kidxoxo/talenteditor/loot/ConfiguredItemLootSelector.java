package com.kidxoxo.talenteditor.loot;

import com.kidxoxo.talenteditor.domain.ConfiguredItemLootEntityTarget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public final class ConfiguredItemLootSelector {
    private ConfiguredItemLootSelector() {
    }

    public static List<Roll> roll(List<ConfiguredItemLootBinding> bindings, RandomSource random) {
        return roll(bindings, ConfiguredItemLootContext.EMPTY, random);
    }

    public static List<Roll> roll(
            List<ConfiguredItemLootBinding> bindings,
            ConfiguredItemLootContext context,
            RandomSource random
    ) {
        ArrayList<Roll> result = new ArrayList<>();
        for (ConfiguredItemLootBinding binding : bindings) {
            if (!matches(binding, context)) continue;
            double chance = binding.rule().effectiveChance(context.luck());
            if (chance <= 0.0D || chance < 1.0D && random.nextDouble() >= chance) continue;
            int minimum = binding.rule().minCount();
            int maximum = binding.rule().maxCount();
            int count = minimum == maximum ? minimum : minimum + random.nextInt(maximum - minimum + 1);
            result.add(new Roll(binding.itemId(), count));
        }
        return List.copyOf(result);
    }

    public static boolean matches(ConfiguredItemLootBinding binding, ConfiguredItemLootContext context) {
        var rule = binding.rule();
        if (!rule.dimensions().isEmpty()
                && context.dimension().filter(rule.dimensions()::contains).isEmpty()) return false;
        if (rule.entityTypes().isEmpty() && rule.entityTags().isEmpty()) return true;
        var entity = rule.entityTarget() == ConfiguredItemLootEntityTarget.THIS_ENTITY
                ? context.thisEntity() : context.killerEntity();
        if (entity.isEmpty()) return false;
        var value = entity.orElseThrow();
        return rule.entityTypes().contains(value.type())
                || value.tags().stream().anyMatch(rule.entityTags()::contains);
    }

    public record Roll(ResourceLocation itemId, int count) {
    }
}
