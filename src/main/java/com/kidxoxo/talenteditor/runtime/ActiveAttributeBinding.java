package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.AttributeModifierAction;
import net.minecraft.resources.ResourceLocation;

public record ActiveAttributeBinding(
        ResourceLocation talentId,
        AttributeModifierAction action
) {
}

