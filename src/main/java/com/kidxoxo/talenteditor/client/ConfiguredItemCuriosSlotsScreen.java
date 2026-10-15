package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.compat.CuriosIntegration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Selects actual player slot types synced by Curios, while retaining unavailable saved choices. */
public final class ConfiguredItemCuriosSlotsScreen extends EditorScreen {
    private static final int ROW_HEIGHT = 19;
    private final ConfiguredItemEditScreen parent;
    private final boolean mainhandOnly;
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();
    private List<String> available = List.of();
    private List<String> shown = List.of();
    private boolean enabled;
    private int scroll;
    private Button enabledButton;
    private Button anyButton;

    ConfiguredItemCuriosSlotsScreen(ConfiguredItemEditScreen parent, boolean enabled,
                                    boolean mainhandOnly, List<String> slots) {
        super(Component.translatable("screen.talent_editor.configured_item.curios_slots.title"));
        this.parent = parent;
        this.enabled = enabled;
        this.mainhandOnly = mainhandOnly;
        selected.addAll(slots);
    }

    @Override protected void init() {
        available = CuriosIntegration.availablePlayerSlots(minecraft == null ? null : minecraft.player);
        LinkedHashSet<String> union = new LinkedHashSet<>(available);
        union.addAll(selected);
        shown = List.copyOf(union);
        int w = Math.min(550, width - 32), x = (width - w) / 2;
        enabledButton = addRenderableWidget(EditorWidgets.button(enabledLabel(), b -> {
            enabled = !enabled;
            enabledButton.setMessage(enabledLabel());
        }).bounds(x, 36, (w - 6) / 2, 20).build());
        enabledButton.active = !mainhandOnly;
        anyButton = addRenderableWidget(EditorWidgets.button(anyLabel(), b -> {
            selected.clear();
            anyButton.setMessage(anyLabel());
        }).bounds(x + (w + 6) / 2, 36, (w - 6) / 2, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, height - 28, (w - 6) / 2, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), b -> apply())
                .bounds(x + (w + 6) / 2, height - 28, (w - 6) / 2, 20).build());
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.configured_item.curios_yes"
                : "screen.talent_editor.configured_item.curios_no");
    }

    private Component anyLabel() {
        return Component.translatable(selected.isEmpty()
                ? "screen.talent_editor.configured_item.curios_slots.any_active"
                : "screen.talent_editor.configured_item.curios_slots.reset_any");
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(550, width - 32), x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 8, 28, w + 16, Math.max(110, height - 66));
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        String help = Component.translatable(mainhandOnly
                ? "screen.talent_editor.configured_item.curios_slots.mainhand_locked"
                : "screen.talent_editor.configured_item.curios_slots.help").getString();
        graphics.drawString(font, font.plainSubstrByWidth(help, w - 8), x + 4, 65, EditorTheme.MUTED, false);
        int top = 84, bottom = height - 44;
        int visible = Math.max(1, (bottom - top) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() - visible)));
        graphics.enableScissor(x, top, x + w, bottom);
        for (int index = scroll; index < Math.min(shown.size(), scroll + visible); index++) {
            int y = top + (index - scroll) * ROW_HEIGHT;
            String slot = shown.get(index);
            boolean checked = selected.contains(slot);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(x + 2, y, x + w - 2, y + ROW_HEIGHT - 1,
                    checked ? 0xA0396C99 : hovered ? 0x80405A75 : 0x80303C4C);
            String translated = Component.translatable("curios.identifier." + slot).getString();
            String label = (checked ? "☑ " : "□ ") + slot
                    + (translated.equals("curios.identifier." + slot) ? "" : "  ·  " + translated)
                    + (available.contains(slot) ? "" : "  ·  " + Component.translatable(
                    "screen.talent_editor.configured_item.curios_slots.unavailable").getString());
            graphics.drawString(font, font.plainSubstrByWidth(label, w - 16), x + 8, y + 5,
                    available.contains(slot) ? EditorTheme.TEXT : EditorTheme.MUTED, false);
        }
        graphics.disableScissor();
        if (shown.isEmpty()) graphics.drawCenteredString(font, Component.translatable(
                "screen.talent_editor.configured_item.curios_slots.none"), width / 2, top + 8,
                EditorTheme.MUTED);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = Math.min(550, width - 32), x = (width - w) / 2;
        int top = 84, bottom = height - 44;
        if (button == 0 && mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < bottom) {
            int index = scroll + (int) ((mouseY - top) / ROW_HEIGHT);
            if (index >= 0 && index < shown.size()) {
                String slot = shown.get(index);
                if (!selected.remove(slot)) selected.add(slot);
                anyButton.setMessage(anyLabel());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = Math.max(1, (height - 44 - 84) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta) * 3,
                Math.max(0, shown.size() - visible)));
        return true;
    }

    private void apply() {
        parent.updateCuriosSlots(enabled, new ArrayList<>(selected));
        onClose();
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
