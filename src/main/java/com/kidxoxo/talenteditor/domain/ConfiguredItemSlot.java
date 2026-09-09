package com.kidxoxo.talenteditor.domain;

import net.minecraft.world.entity.EquipmentSlot;

import java.util.Locale;

public enum ConfiguredItemSlot {
    MAINHAND(EquipmentSlot.MAINHAND),
    OFFHAND(EquipmentSlot.OFFHAND),
    HEAD(EquipmentSlot.HEAD),
    CHEST(EquipmentSlot.CHEST),
    LEGS(EquipmentSlot.LEGS),
    FEET(EquipmentSlot.FEET);

    private final EquipmentSlot equipmentSlot;

    ConfiguredItemSlot(EquipmentSlot equipmentSlot) {
        this.equipmentSlot = equipmentSlot;
    }

    public EquipmentSlot equipmentSlot() {
        return equipmentSlot;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ConfiguredItemSlot parse(String value) {
        return valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
