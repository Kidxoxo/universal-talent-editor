package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.NumericExpression;
import com.kidxoxo.talenteditor.domain.NearbyEntityQuery;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class NumericExpressionRuntime {
    private NumericExpressionRuntime() {
    }

    public static double evaluate(
            NumericExpression expression,
            ServerPlayer owner,
            Entity expressionTarget,
            double eventAmount,
            double currentDamage,
            ResourceLocation talentId
    ) {
        return expression.evaluate(resolver(owner, expressionTarget, eventAmount, currentDamage, talentId));
    }

    public static NumericExpression.Resolver resolver(
            ServerPlayer owner,
            Entity expressionTarget,
            double eventAmount,
            double currentDamage,
            ResourceLocation talentId
    ) {
        LivingEntity target = expressionTarget instanceof LivingEntity living ? living : null;
        return new NumericExpression.Resolver() {
            @Override
            public double variable(String name) {
                return switch (name) {
                    case "level" -> owner.experienceLevel;
                    case "experience_progress" -> owner.experienceProgress;
                    case "health" -> owner.getHealth();
                    case "max_health" -> owner.getMaxHealth();
                    case "health_percent" -> owner.getMaxHealth() <= 0.0F ? 0.0D : owner.getHealth() / owner.getMaxHealth();
                    case "hunger" -> owner.getFoodData().getFoodLevel();
                    case "hunger_percent" -> owner.getFoodData().getFoodLevel() / 20.0D;
                    case "saturation" -> owner.getFoodData().getSaturationLevel();
                    case "current_damage" -> currentDamage;
                    case "event_amount" -> eventAmount;
                    case "distance_to_target" -> target == null ? 0.0D : owner.distanceTo(target);
                    case "target_health" -> target == null ? 0.0D : target.getHealth();
                    case "target_max_health" -> target == null ? 0.0D : target.getMaxHealth();
                    case "target_health_percent" -> target == null || target.getMaxHealth() <= 0.0F
                            ? 0.0D : target.getHealth() / target.getMaxHealth();
                    case "online_players" -> owner.server.getPlayerCount();
                    case "owned_count" -> PlayerTalents.owned(owner).size();
                    case "world_time" -> owner.serverLevel().getGameTime();
                    case "stationary_ticks" -> PlayerMotionTracker.stationaryTicks(owner);
                    case "stationary_seconds" -> PlayerMotionTracker.stationaryTicks(owner) / 20.0D;
                    case "movement_distance_tick" -> PlayerMotionTracker.lastMovement(owner);
                    case "talent_age_ticks" -> talentAge(owner, talentId);
                    case "draws_since_acquired" -> drawsSinceAcquired(owner, talentId);
                    default -> 0.0D;
                };
            }

            @Override
            public double lookup(String function, String key) {
                if (function.equals("nearby_count")) return nearbyCount(owner, key);
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null) return 0.0D;
                return switch (function) {
                    case "counter" -> PlayerTalents.counter(owner, id);
                    case "state" -> PlayerTalents.numericState(owner, id);
                    case "target_state" -> target == null ? 0.0D : EntityNumericStateService.get(target, id);
                    case "attribute" -> attribute(owner, id);
                    case "target_attribute" -> target == null ? 0.0D : attribute(target, id);
                    case "attribute_base" -> attributeBase(owner, id);
                    case "attribute_ratio_bonus" -> attributeRatioBonus(owner, id);
                    case "category_count" -> categoryCount(owner, id);
                    case "rarity_count" -> rarityCount(owner, id);
                    case "ever_category_count" -> historyCategoryCount(owner, id);
                    case "ever_rarity_count" -> historyRarityCount(owner, id);
                    case "has_talent" -> PlayerTalents.owned(owner).contains(id) ? 1.0D : 0.0D;
                    case "has_ever_talent" -> PlayerTalents.hasEverAcquired(owner, id) ? 1.0D : 0.0D;
                    case "has_item" -> countItem(owner, id);
                    default -> 0.0D;
                };
            }

            @Override
            public double random() {
                return owner.getRandom().nextDouble();
            }
        };
    }

    private static double talentAge(ServerPlayer player, ResourceLocation talent) {
        if (talent == null) return 0.0D;
        long acquired = PlayerTalents.acquiredTick(player, talent);
        return acquired < 0L ? 0.0D : Math.max(0L, PlayerTalents.counterLifecycleTicks(player) - acquired);
    }

    private static double drawsSinceAcquired(ServerPlayer player, ResourceLocation talent) {
        if (talent == null) return 0.0D;
        long acquired = PlayerTalents.acquiredDraw(player, talent);
        return acquired < 0L ? 0.0D : Math.max(0L, PlayerTalents.drawSelections(player) - acquired);
    }

    private static double attribute(LivingEntity entity, ResourceLocation id) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
        return attribute == null || entity.getAttribute(attribute) == null ? 0.0D : entity.getAttributeValue(attribute);
    }

    private static double attributeBase(LivingEntity entity, ResourceLocation id) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
        var instance = attribute == null ? null : entity.getAttribute(attribute);
        return instance == null ? 0.0D : instance.getBaseValue();
    }

    private static double attributeRatioBonus(LivingEntity entity, ResourceLocation id) {
        double base = attributeBase(entity, id);
        return Math.abs(base) < 1.0E-12D ? 0.0D : attribute(entity, id) / base - 1.0D;
    }

    private static long categoryCount(ServerPlayer player, ResourceLocation category) {
        return PlayerTalents.categorySources(player).stream()
                .map(TalentEditorMod.SNAPSHOTS.active().talents()::get)
                .filter(java.util.Objects::nonNull)
                .filter(talent -> talent.enabled() && talent.available() && talent.categories().contains(category))
                .count();
    }

    private static long rarityCount(ServerPlayer player, ResourceLocation rarity) {
        return PlayerTalents.categorySources(player).stream()
                .map(TalentEditorMod.SNAPSHOTS.active().talents()::get)
                .filter(java.util.Objects::nonNull)
                .filter(talent -> talent.enabled() && talent.available() && talent.rarity().equals(rarity))
                .count();
    }

    private static long historyCategoryCount(ServerPlayer player, ResourceLocation category) {
        return PlayerTalents.talentHistory(player).stream()
                .map(TalentEditorMod.SNAPSHOTS.active().talents()::get)
                .filter(java.util.Objects::nonNull)
                .filter(talent -> talent.categories().contains(category))
                .count();
    }

    private static long historyRarityCount(ServerPlayer player, ResourceLocation rarity) {
        return PlayerTalents.talentHistory(player).stream()
                .map(TalentEditorMod.SNAPSHOTS.active().talents()::get)
                .filter(java.util.Objects::nonNull)
                .filter(talent -> talent.rarity().equals(rarity))
                .count();
    }

    private static long nearbyCount(ServerPlayer owner, String source) {
        NearbyEntityQuery query;
        try {
            query = NearbyEntityQuery.parse(source);
        } catch (IllegalArgumentException exception) {
            return 0L;
        }
        return owner.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                owner.getBoundingBox().inflate(query.radius()),
                entity -> nearbyMatches(owner, entity, query)
        ).stream().limit(4_096L).count();
    }

    private static boolean nearbyMatches(
            ServerPlayer owner,
            LivingEntity entity,
            NearbyEntityQuery query
    ) {
        if (entity == owner && !query.includeSelf()) return false;
        if (!entity.isAlive()) return false;
        if (query.kind() == NearbyEntityQuery.Kind.PLAYER && !(entity instanceof Player)) return false;
        if (query.relation() == com.kidxoxo.talenteditor.domain.EntityRelation.ALLY
                && !owner.isAlliedTo(entity)) return false;
        if (query.relation() == com.kidxoxo.talenteditor.domain.EntityRelation.ENEMY
                && owner.isAlliedTo(entity)) return false;
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (query.type().isPresent() && !query.type().orElseThrow().equals(type)) return false;
        return query.tag().isEmpty() || entity.getType().builtInRegistryHolder().tags()
                .anyMatch(tag -> tag.location().equals(query.tag().orElseThrow()));
    }

    private static int countItem(ServerPlayer player, ResourceLocation id) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            ResourceLocation actual = ConfiguredItem.definitionId(stack)
                    .orElseGet(() -> ForgeRegistries.ITEMS.getKey(stack.getItem()));
            if (id.equals(actual)) count += stack.getCount();
        }
        return count;
    }
}
