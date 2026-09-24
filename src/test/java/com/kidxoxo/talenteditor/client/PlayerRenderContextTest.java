package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRenderContextTest {

    @AfterEach
    void drain() {
        while (PlayerRenderContext.pop() != null) {
            // discard frames left behind by a failing test
        }
    }

    @Test
    void emptyStackHasNoCurrentPlayer() {
        assertNull(PlayerRenderContext.current());
        assertNull(PlayerRenderContext.pop());
    }

    @Test
    void framesAreRestoredInReverseOrder() {
        PlayerRenderContext.push(null, false);
        PlayerRenderContext.push(null, true);

        PlayerRenderContext.Frame inner = PlayerRenderContext.pop();
        assertNotNull(inner);
        assertTrue(inner.scaled());
        assertNull(inner.player());

        PlayerRenderContext.Frame outer = PlayerRenderContext.pop();
        assertNotNull(outer);
        assertFalse(outer.scaled());

        assertNull(PlayerRenderContext.pop());
    }

    @Test
    void nonPlayerFrameReportsNoCurrentPlayer() {
        PlayerRenderContext.push(null, false);
        assertNull(PlayerRenderContext.current());
    }

    @Test
    void nestedFramesReturnTheInnermostPlayer() {
        PlayerRenderContext.push(null, false);
        PlayerRenderContext.push(null, true);

        assertEquals(true, PlayerRenderContext.pop().scaled());
        assertEquals(false, PlayerRenderContext.pop().scaled());
    }
}
