package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.Event;
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
        ClientCreativeItems.refreshIfNeeded();
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

    @SubscribeEvent
    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;
        var state = ClientEditorState.playerTalentState();
        if (state == null || state.hudResources().isEmpty()) return;
        int shown = 0;
        for (var resource : state.hudResources()) {
            double ratio = Math.max(0.0D, Math.min(1.0D, resource.value() / resource.maximum()));
            if (!resource.showWhenFull() && ratio >= 1.0D) continue;
            int width = 164;
            int x = 8;
            int y = event.getWindow().getGuiScaledHeight() - 20 - shown * 13;
            event.getGuiGraphics().fill(x, y, x + width, y + 9, 0xB0000000);
            int filled = (int) Math.round((width - 2) * ratio);
            event.getGuiGraphics().fill(x + 1, y + 1, x + 1 + filled, y + 8,
                    0xFF000000 | resource.color());
            String label = resource.name().component(0xFFFFFF).getString() + " " + compact(resource.value())
                    + "/" + compact(resource.maximum());
            event.getGuiGraphics().drawCenteredString(Minecraft.getInstance().font, label,
                    x + width / 2, y, 0xFFFFFFFF);
            if (++shown >= 8) break;
        }
    }

    @SubscribeEvent
    public static void renderNameplate(RenderNameTagEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && ClientVisualState.get(player.getUUID()).hideNameplate()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientVisualState.clear();
        ClientCreativeItems.clear();
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
