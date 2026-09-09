package com.kidxoxo.talenteditor.catalog;

import net.minecraft.resources.ResourceLocation;

public record AttributeDescriptor(
        ResourceLocation id,
        String descriptionId,
        double defaultValue,
        Double minValue,
        Double maxValue,
        boolean clientSyncable,
        boolean attachedToTarget
) {
}

