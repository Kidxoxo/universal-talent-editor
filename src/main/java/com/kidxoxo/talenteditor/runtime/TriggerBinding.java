package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.CompiledTrigger;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import net.minecraft.resources.ResourceLocation;

public record TriggerBinding(
        DefinitionKind sourceKind,
        ResourceLocation talentId,
        CompiledTrigger trigger
) {
    public TriggerBinding {
        if (sourceKind != DefinitionKind.TALENT && sourceKind != DefinitionKind.ITEM)
            throw new IllegalArgumentException("Trigger source must be a talent or configured item");
    }

    public TriggerBinding(ResourceLocation talentId, CompiledTrigger trigger) {
        this(DefinitionKind.TALENT, talentId, trigger);
    }

    public boolean ownedBy(java.util.Set<ResourceLocation> talents, java.util.Set<ResourceLocation> items) {
        return (sourceKind == DefinitionKind.ITEM ? items : talents).contains(talentId);
    }

    public String stateKey() {
        return (sourceKind == DefinitionKind.ITEM ? "item|" : "") + talentId + "#" + trigger.id();
    }
}

