package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicAttributeModifierActionTest {
    @Test
    void evaluatesAndClampsLinearFormula() {
        DynamicAttributeModifierAction action = new DynamicAttributeModifierAction(
                new ResourceLocation("minecraft", "generic.attack_damage"),
                DynamicAttributeSource.COUNTER,
                Optional.of(new ResourceLocation("example", "kills")),
                1.0D, 0.5D, -2.0D, 6.0D,
                AttributeOperation.ADDITION, "kills_damage", true
        );

        assertEquals(1.0D, action.amount(0.0D));
        assertEquals(3.5D, action.amount(5.0D));
        assertEquals(6.0D, action.amount(100.0D));
        assertEquals(-2.0D, action.amount(-100.0D));
    }
}
