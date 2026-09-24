package com.kidxoxo.talenteditor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumericExpressionTest {
    @Test
    void comparisonFunctionsProduceExactPrideLayerCounts() {
        NumericExpression.Resolver empty = name -> 0.0D;
        assertEquals(2.0D, NumericExpression.compile("gt(20,10)+gt(5,6)+lt(3,4)")
                .evaluate(empty), 1.0E-9D);
        assertEquals(0.0D, NumericExpression.compile("eq(2,3)+gte(1,2)+lte(3,2)")
                .evaluate(empty), 1.0E-9D);
    }
    @Test
    void evaluatesArithmeticFunctionsAndDataLookups() {
        NumericExpression expression = NumericExpression.compile(
                "clamp(counter(\"example:kills\") * 0.5 + max(level, 3), 0, 20)"
        );
        NumericExpression.Resolver resolver = new NumericExpression.Resolver() {
            @Override public double variable(String name) { return name.equals("level") ? 4.0D : 0.0D; }
            @Override public double lookup(String function, String key) {
                return function.equals("counter") && key.equals("example:kills") ? 12.0D : 0.0D;
            }
        };
        assertEquals(10.0D, expression.evaluate(resolver), 1.0E-9D);
    }

    @Test
    void handlesPowerUnaryAndSafeDivision() {
        NumericExpression.Resolver empty = name -> 0.0D;
        assertEquals(7.0D, NumericExpression.compile("-2 + 3^2").evaluate(empty), 1.0E-9D);
        assertEquals(0.0D, NumericExpression.compile("5 / 0").evaluate(empty), 1.0E-9D);
        assertEquals(3.0D, NumericExpression.compile("sqrt(9)").evaluate(empty), 1.0E-9D);
    }

    @Test
    void rejectsScriptsUnknownFunctionsAndMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> NumericExpression.compile("java.lang.Runtime()"));
        assertThrows(IllegalArgumentException.class, () -> NumericExpression.compile("execute(1)"));
        assertThrows(IllegalArgumentException.class, () -> NumericExpression.compile("counter(example:kills)"));
        assertThrows(IllegalArgumentException.class, () -> NumericExpression.compile("1 +"));
        assertThrows(IllegalArgumentException.class, () -> NumericExpression.compile(
                "nearby_count(\"radius=999;kind=player\")"
        ));
    }

    @Test
    void acceptsHistoryAndBoundedNearbyLookups() {
        NumericExpression expression = NumericExpression.compile(
                "ever_rarity_count(\"example:ur\") + "
                        + "nearby_count(\"radius=100;kind=player;relation=any\")"
        );
        assertEquals(5.0D, expression.evaluate(new NumericExpression.Resolver() {
            @Override public double variable(String name) { return 0.0D; }
            @Override public double lookup(String function, String key) {
                return function.equals("ever_rarity_count") ? 2.0D : 3.0D;
            }
        }), 1.0E-9D);
    }

    @Test
    void acceptsGenericAttributeBaseAndRatioLookups() {
        NumericExpression expression = NumericExpression.compile(
                "attribute_base(\"minecraft:generic.max_health\") + "
                        + "attribute_ratio_bonus(\"minecraft:generic.max_health\") * 20"
        );
        assertEquals(30.0D, expression.evaluate(new NumericExpression.Resolver() {
            @Override public double variable(String name) { return 0.0D; }
            @Override public double lookup(String function, String key) {
                return function.equals("attribute_base") ? 20.0D : 0.5D;
            }
        }), 1.0E-9D);
    }
}
