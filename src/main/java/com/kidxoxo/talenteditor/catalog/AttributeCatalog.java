package com.kidxoxo.talenteditor.catalog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;
import java.util.List;

public final class AttributeCatalog {
    private AttributeCatalog() {
    }

    public static List<AttributeDescriptor> capture(LivingEntity target) {
        return ForgeRegistries.ATTRIBUTES.getValues().stream()
                .map(attribute -> describe(attribute, target))
                .filter(descriptor -> descriptor.id() != null)
                .sorted(Comparator.comparing(descriptor -> descriptor.id().toString()))
                .toList();
    }

    private static AttributeDescriptor describe(Attribute attribute, LivingEntity target) {
        ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        Double min = null;
        Double max = null;
        if (attribute instanceof RangedAttribute ranged) {
            min = ranged.getMinValue();
            max = ranged.getMaxValue();
        }
        return new AttributeDescriptor(
                id,
                attribute.getDescriptionId(),
                attribute.getDefaultValue(),
                min,
                max,
                attribute.isClientSyncable(),
                target != null && target.getAttribute(attribute) != null
        );
    }
}

