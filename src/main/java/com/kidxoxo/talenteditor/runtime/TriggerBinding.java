package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.CompiledTrigger;
import net.minecraft.resources.ResourceLocation;

public record TriggerBinding(
        ResourceLocation talentId,
        CompiledTrigger trigger
) {
}

