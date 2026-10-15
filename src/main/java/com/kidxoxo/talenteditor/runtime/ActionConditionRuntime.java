package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.ConditionalAction;
import com.kidxoxo.talenteditor.domain.NumericExpression;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Evaluates action-local gates against the current owner and event context. */
public final class ActionConditionRuntime {
    private ActionConditionRuntime() { }

    public static boolean active(CompiledAction action, ServerPlayer owner, ResourceLocation talentId) {
        return active(action, owner, null, 0.0D, 0.0D, talentId);
    }

    public static boolean active(CompiledAction action, ServerPlayer owner, Entity eventEntity,
                                 double eventAmount, double currentDamage, ResourceLocation talentId) {
        if (!hasConditions(action)) return true;
        var context = eventEntity == null ? ConditionEvaluator.context(owner)
                : ConditionEvaluator.eventContext(owner, eventEntity, null, ItemStack.EMPTY, null, null);
        var resolver = NumericExpressionRuntime.resolver(owner, eventEntity, eventAmount, currentDamage, talentId);
        return active(action, context, resolver);
    }

    public static boolean active(CompiledAction action, ConditionEvaluator.Context context,
                                 NumericExpression.Resolver resolver) {
        if (!(action instanceof ConditionalAction conditional)) return true;
        return conditional.enabledWhen().map(condition -> ConditionEvaluator.test(condition, context, resolver))
                .orElse(true)
                && conditional.disabledWhen().map(condition -> !ConditionEvaluator.test(condition, context, resolver))
                .orElse(true);
    }

    public static boolean active(CompiledCondition enabledWhen, CompiledCondition disabledWhen,
                                 ServerPlayer owner, ResourceLocation talentId) {
        if (enabledWhen == null && disabledWhen == null) return true;
        var context = ConditionEvaluator.context(owner);
        var resolver = NumericExpressionRuntime.resolver(owner, null, 0.0D, 0.0D, talentId);
        return (enabledWhen == null || ConditionEvaluator.test(enabledWhen, context, resolver))
                && (disabledWhen == null || !ConditionEvaluator.test(disabledWhen, context, resolver));
    }

    static boolean hasConditions(CompiledAction action) {
        return action instanceof ConditionalAction gate
                && (gate.enabledWhen().isPresent() || gate.disabledWhen().isPresent());
    }
}
