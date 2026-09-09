package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        SynthesisSoundPlayback.tick(minecraft);
        while (ClientModEvents.OPEN_EDITOR.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                EditorNetwork.requestOpen();
            }
        }
        while (ClientModEvents.OPEN_TALENT_BOOK.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                EditorNetwork.requestTalentBook();
            }
        }
    }
}
