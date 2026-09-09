package com.kidxoxo.talenteditor.settings;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EditorServerEvents {
    private EditorServerEvents() {
    }

    @SubscribeEvent
    public static void serverAboutToStart(ServerAboutToStartEvent event) {
        if (!TalentEditorMod.SETTINGS.initializeServer(event.getServer())) return;
        try {
            TalentEditorMod.EDITOR_PACK.ensureSelected(event.getServer()).whenComplete((ignored, error) -> {
                if (error != null) TalentEditorMod.LOGGER.error(
                        "Installed the global talent template but could not enable its data pack", error
                );
            });
        } catch (Exception exception) {
            TalentEditorMod.LOGGER.error(
                    "Installed the global talent template but could not enable its data pack", exception
            );
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        TalentEditorMod.SETTINGS.clearServer();
    }
}
