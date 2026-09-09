package com.kidxoxo.talenteditor.item;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A fixed-function built-in item whose appearance and equipment attributes use item configuration. */
abstract class SystemConfiguredItem extends Item {
    SystemConfiguredItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        ConfiguredItem.refreshDefinition(stack);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide()) ConfiguredItem.refreshDefinition(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return ConfiguredItem.configuredFoil(stack) || super.isFoil(stack);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return ConfiguredItem.configuredAttributeModifiers(super.getAttributeModifiers(slot, stack), slot, stack);
    }
}
