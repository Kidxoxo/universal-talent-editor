package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

/** One attribute modifier may apply in several equipment slots. */
final class ConfiguredItemAttributeSlotsScreen extends EditorScreen {
    private static final List<String> SLOTS = List.of(
            "mainhand", "offhand", "head", "chest", "legs", "feet", "curios");
    private static final int ROW_HEIGHT = 24;
    private final ConfiguredItemAttributeScreen parent;
    private final Consumer<List<String>> selection;
    private final LinkedHashSet<String> checked = new LinkedHashSet<>();
    private String status = "";

    ConfiguredItemAttributeSlotsScreen(ConfiguredItemAttributeScreen parent, List<String> slots,
                                       Consumer<List<String>> selection) {
        super(Component.translatable("screen.talent_editor.configured_item.attributes.slots_title"));
        this.parent = parent;
        this.selection = selection;
        checked.addAll(slots);
    }

    @Override protected void init() {
        int w = Math.min(420, width - 32), x = (width - w) / 2;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, height - 28, (w - 6) / 2, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), b -> apply())
                .bounds(x + (w + 6) / 2, height - 28, (w - 6) / 2, 20).build());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(420, width - 32), x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 8, 29, w + 16, Math.min(height - 65, 230));
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.attributes.slots_help"), x + 4, 39,
                EditorTheme.MUTED, false);
        for (int i = 0; i < SLOTS.size(); i++) {
            int y = 56 + i * ROW_HEIGHT;
            String slot = SLOTS.get(i);
            boolean selected = checked.contains(slot);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(x + 2, y, x + w - 2, y + ROW_HEIGHT - 2,
                    selected ? 0xFF28547C : hovered ? 0xFF25394F : 0xFF172738);
            String label = (selected ? "☑ " : "□ ") + Component.translatable(
                    "screen.talent_editor.configured_item.attributes.slot." + slot).getString();
            graphics.drawString(font, label, x + 10, y + 7, EditorTheme.TEXT, false);
        }
        if (!status.isBlank()) graphics.drawString(font, status, x + 4, 56 + SLOTS.size() * ROW_HEIGHT + 3,
                0xFFFF7777, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int w = Math.min(420, width - 32), x = (width - w) / 2;
        if (button == 0 && mouseX >= x && mouseX < x + w
                && mouseY >= 56 && mouseY < 56 + SLOTS.size() * ROW_HEIGHT) {
            String slot = SLOTS.get((int) ((mouseY - 56) / ROW_HEIGHT));
            if (!checked.remove(slot)) checked.add(slot);
            status = "";
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void apply() {
        if (checked.isEmpty()) {
            status = Component.translatable("screen.talent_editor.configured_item.attributes.invalid_slots")
                    .getString();
            return;
        }
        selection.accept(new ArrayList<>(checked));
        onClose();
    }

    @Override public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
