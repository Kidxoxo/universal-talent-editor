package com.kidxoxo.talenteditor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AffectHealthExpressionActionTest {
    @Test
    void optionallyTurnsNegativeDamageIntoHealing() {
        AffectHealthExpressionAction signed = new AffectHealthExpressionAction(
                HealthOperation.DAMAGE, NumericExpression.compile("-4"), 10.0D, true,
                new EntitySelectorSpec(
                        LivingEntityTarget.SELF, 4.0D, EntityRelation.ANY, EntityOrder.NEAREST, 1
                )
        );
        assertEquals(HealthOperation.HEAL, signed.resolvedOperation(-4.0D));
        assertEquals(4.0D, signed.boundedMagnitude(-4.0D));
        assertEquals(10.0D, signed.boundedMagnitude(-20.0D));

        AffectHealthExpressionAction legacy = new AffectHealthExpressionAction(
                HealthOperation.DAMAGE, NumericExpression.compile("-4"), 10.0D, false,
                new EntitySelectorSpec(
                        LivingEntityTarget.SELF, 4.0D, EntityRelation.ANY, EntityOrder.NEAREST, 1
                )
        );
        assertEquals(HealthOperation.DAMAGE, legacy.resolvedOperation(-4.0D));
        assertEquals(0.0D, legacy.boundedMagnitude(-4.0D));
    }
}
