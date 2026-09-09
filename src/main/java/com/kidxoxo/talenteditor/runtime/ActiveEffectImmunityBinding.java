package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.EffectImmunityAction;
import net.minecraft.resources.ResourceLocation;

public record ActiveEffectImmunityBinding(
        ResourceLocation talentId,
        EffectImmunityAction action
) {
}
