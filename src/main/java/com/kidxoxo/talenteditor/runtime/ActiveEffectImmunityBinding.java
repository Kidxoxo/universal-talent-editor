package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.EffectImmunityAction;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import net.minecraft.resources.ResourceLocation;

public record ActiveEffectImmunityBinding(
        ResourceLocation talentId,
        EffectImmunityAction action,
        CompiledCondition enabledWhen,
        CompiledCondition disabledWhen
) {
    public ActiveEffectImmunityBinding(ResourceLocation talentId, EffectImmunityAction action) {
        this(talentId, action, null, null);
    }
}
