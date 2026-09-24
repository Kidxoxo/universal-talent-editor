package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.EntityOrder;
import com.kidxoxo.talenteditor.domain.EntityRelation;
import com.kidxoxo.talenteditor.domain.EntitySelectorSpec;
import com.kidxoxo.talenteditor.domain.LivingEntityTarget;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EntityTargetSelector {
    private EntityTargetSelector() {
    }

    public static List<LivingEntity> select(
            EntitySelectorSpec selector,
            ServerPlayer owner,
            Entity eventEntity,
            DamageSource damageSource
    ) {
        if (selector.target() != LivingEntityTarget.NEARBY) {
            Entity selected = switch (selector.target()) {
                case SELF -> owner;
                case EVENT_ENTITY -> eventEntity;
                case DAMAGE_SOURCE -> damageSource == null ? null : damageSource.getEntity();
                case NEARBY -> null;
            };
            return selected instanceof LivingEntity living && living.isAlive() ? List.of(living) : List.of();
        }
        ArrayList<LivingEntity> values = new ArrayList<>(owner.serverLevel().getEntitiesOfClass(
                LivingEntity.class, owner.getBoundingBox().inflate(selector.radius()),
                entity -> entity != owner && entity.isAlive() && relationMatches(selector.relation(), owner, entity)
        ));
        if (selector.order() == EntityOrder.RANDOM) {
            java.util.Collections.shuffle(values, new java.util.Random(owner.getRandom().nextLong()));
        } else {
            Comparator<LivingEntity> comparator = Comparator.comparingDouble(owner::distanceToSqr);
            if (selector.order() == EntityOrder.FARTHEST) comparator = comparator.reversed();
            values.sort(comparator.thenComparing(entity -> entity.getUUID().toString()));
        }
        if (values.size() > selector.limit()) values.subList(selector.limit(), values.size()).clear();
        return List.copyOf(values);
    }

    private static boolean relationMatches(EntityRelation relation, ServerPlayer owner, LivingEntity entity) {
        return switch (relation) {
            case ANY -> true;
            case ALLY -> owner.isAlliedTo(entity);
            case ENEMY -> !owner.isAlliedTo(entity);
        };
    }
}
