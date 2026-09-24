package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import net.minecraft.resources.ResourceLocation;

public record ActiveStatusEffectBinding(
        ResourceLocation talentId,
        AddEffectAction action,
        CompiledCondition enabledWhen,
        CompiledCondition disabledWhen
) {
    public ActiveStatusEffectBinding(ResourceLocation talentId, AddEffectAction action) {
        this(talentId, action, null, null);
    }
}
