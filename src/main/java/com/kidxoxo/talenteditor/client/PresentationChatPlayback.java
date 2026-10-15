package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.PresentationChatTiming;
import com.kidxoxo.talenteditor.network.PresentationChatPacket;
import net.minecraft.client.Minecraft;

public final class PresentationChatPlayback {
    private static int timestampOffset;
    private PresentationChatPlayback() {}

    public static void accept(PresentationChatPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        int previous = timestampOffset;
        timestampOffset = PresentationChatTiming.timestampOffset(packet.visibleTicks());
        try {
            // Use vanilla insertion: line wrapping, styled text, logging and history remain intact.
            minecraft.gui.getChat().addMessage(packet.text());
        } finally {
            timestampOffset = previous;
        }
    }

    public static int timestamp(int vanillaTimestamp) {
        return vanillaTimestamp + timestampOffset;
    }
}
