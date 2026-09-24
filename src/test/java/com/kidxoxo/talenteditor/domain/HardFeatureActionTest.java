package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HardFeatureActionTest {
    private static final ResourceLocation DIAMOND = new ResourceLocation("minecraft", "diamond");

    @Test
    void normalizesHudColorAndAcceptsRenderExpressions() {
        HudResourceAction hud = new HudResourceAction(
                new ResourceLocation("example", "energy"), NumericExpression.compile("20 + level"),
                0xFF336699, 4, true
        );
        assertEquals(0x336699, hud.color());
        assertEquals("20 + level", hud.maximum().source());
        PlayerRenderAction render = new PlayerRenderAction(
                NumericExpression.compile("1-counter(\"example:transparency\")/100"),
                NumericExpression.compile("1"), false
        );
        assertEquals("1-counter(\"example:transparency\")/100", render.alpha().source());
    }

    @Test
    void acceptsBoundedSubmissionActions() {
        OpenItemSubmissionAction submission = new OpenItemSubmissionAction(
                DIAMOND, NumericExpression.compile("2"), 200, true
        );
        assertEquals(200, submission.timeoutTicks());
        assertThrows(IllegalArgumentException.class, () -> new OpenItemSubmissionAction(
                DIAMOND, NumericExpression.compile("1"), 19, false
        ));
    }
}
