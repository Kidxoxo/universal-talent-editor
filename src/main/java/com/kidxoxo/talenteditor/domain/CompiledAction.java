package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

public sealed interface CompiledAction permits
        AttributeModifierAction, DynamicAttributeModifierAction, CreativeFlightAction,
        AddEffectAction, RemoveEffectAction, EffectImmunityAction, ModifyDamageAction,
        CancelDamageAction, PlaySoundAction, ShowMessageAction, DrawRefreshBonusAction,
        QueueDrawConstraintAction, ModifyEntityAttributeAction, DamageEntityAction,
        HealEntityAction {
    ResourceLocation type();
}
