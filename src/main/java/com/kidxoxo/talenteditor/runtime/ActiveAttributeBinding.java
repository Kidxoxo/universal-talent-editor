package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.AttributeModifierAction;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import net.minecraft.resources.ResourceLocation;

public record ActiveAttributeBinding(
        ResourceLocation talentId,
        AttributeModifierAction action,
        CompiledCondition enabledWhen,
        CompiledCondition disabledWhen
) {
    public ActiveAttributeBinding(ResourceLocation talentId, AttributeModifierAction action) {
        this(talentId, action, null, null);
    }
}
