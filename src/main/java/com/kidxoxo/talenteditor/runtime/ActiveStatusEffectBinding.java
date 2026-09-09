package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.AddEffectAction;
import net.minecraft.resources.ResourceLocation;

public record ActiveStatusEffectBinding(
        ResourceLocation talentId,
        AddEffectAction action
) {
}
