package com.kidxoxo.talenteditor.settings;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Files;

@Mod.EventBusSubscriber(modid = TalentEditorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EditorServerEvents {
    private EditorServerEvents() {
    }

    @SubscribeEvent
    public static void serverAboutToStart(ServerAboutToStartEvent event) {
        TalentEditorMod.SETTINGS.initializeServer(event.getServer());
        if (!Files.isDirectory(TalentEditorMod.EDITOR_PACK.datapacksRoot(event.getServer())
                .resolve(com.kidxoxo.talenteditor.editor.EditorPackStorage.PACK_FOLDER))) return;
        try {
            TalentEditorMod.EDITOR_PACK.ensureSelected(event.getServer()).whenComplete((ignored, error) -> {
                if (error != null) TalentEditorMod.LOGGER.error(
                        "Could not enable the generated talent editor data pack", error
                );
            });
        } catch (Exception exception) {
            TalentEditorMod.LOGGER.error(
                    "Could not enable the generated talent editor data pack", exception
            );
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        TalentEditorMod.SETTINGS.clearServer();
    }
}
