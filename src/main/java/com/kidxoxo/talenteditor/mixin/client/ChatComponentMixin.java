package com.kidxoxo.talenteditor.mixin.client;

import com.kidxoxo.talenteditor.client.PresentationChatPlayback;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Scoped to presentation insertion; normal chat keeps its original timestamp and fade. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyArg(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V"), index = 2)
    private int talentEditor$chatHold(int vanillaTimestamp) {
        return PresentationChatPlayback.timestamp(vanillaTimestamp);
    }
}
