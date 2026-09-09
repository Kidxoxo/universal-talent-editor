package com.kidxoxo.talenteditor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModifyDamageActionTest {
    @Test
    void appliesSequentialOperationAndBoundsWithoutProducingInvalidDamage() {
        ModifyDamageAction add = new ModifyDamageAction(DamageOperation.ADD, -5.0D, 1.0D, 100.0D);
        assertEquals(5.0F, add.apply(10.0F));
        assertEquals(1.0F, add.apply(2.0F));

        ModifyDamageAction multiply = new ModifyDamageAction(
                DamageOperation.MULTIPLY, 2.5D, 0.0D, 20.0D
        );
        assertEquals(20.0F, multiply.apply(10.0F));
        assertEquals(0.0F, multiply.apply(Float.NaN));

        assertThrows(IllegalArgumentException.class, () -> new ModifyDamageAction(
                DamageOperation.MULTIPLY, -1.0D, 0.0D, 10.0D
        ));
        assertThrows(IllegalArgumentException.class, () -> new ModifyDamageAction(
                DamageOperation.ADD, 1.0D, 10.0D, 5.0D
        ));
    }
}
