package com.kidxoxo.talenteditor.client;

import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks which player is currently being rendered by {@code LivingEntityRenderer}.
 *
 * <p>Render order is strictly nested on the client render thread, so a per-thread stack is enough.
 * Non-player living entities push a frame with a {@code null} player so the push/pop pairing used by
 * the mixin always stays balanced.</p>
 */
public final class PlayerRenderContext {
    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private PlayerRenderContext() {
    }

    public static void push(Player player, boolean scaled) {
        STACK.get().push(new Frame(player, scaled));
    }

    public static Frame pop() {
        Deque<Frame> stack = STACK.get();
        Frame frame = stack.poll();
        if (stack.isEmpty()) STACK.remove();
        return frame;
    }

    public static Player current() {
        Frame frame = STACK.get().peek();
        return frame == null ? null : frame.player();
    }

    public record Frame(Player player, boolean scaled) {
    }
}
