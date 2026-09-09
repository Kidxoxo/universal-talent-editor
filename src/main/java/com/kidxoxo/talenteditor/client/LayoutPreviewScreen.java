package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LayoutPreviewScreen extends Screen {
    private final Screen parent;
    private final TalentBookLayoutDefinition layout;
    private int previewPage;
    private Button previousPageButton;
    private Button nextPageButton;

    public LayoutPreviewScreen(Screen parent, TalentBookLayoutDefinition layout) {
        super(Component.translatable("screen.talent_editor.layout.preview_title"));
        this.parent = parent;
        this.layout = layout;
    }

    @Override
    protected void init() {
        previousPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(16, height - 28, 28, 20).build());
        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(48, height - 28, 28, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnParent())
                .bounds(width / 2 - 40, height - 28, 80, 20).build());
        updatePageButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        java.util.Optional<net.minecraft.resources.ResourceLocation> background = previewBackground();
        boolean backgroundMissing = background.isPresent()
                && minecraft.getResourceManager().getResource(background.orElseThrow()).isEmpty();
        background.flatMap(id -> minecraft.getResourceManager().getResource(id)).ifPresent(resource ->
                graphics.blit(background.orElseThrow(), 0, 0, 0.0F, 0.0F,
                        width, height, width, height)
        );
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 9, 0xFFFFFF);

        int left = 18;
        int top = 30;
        int right = width - 18;
        int bottom = height - 48;
        graphics.fill(left, top, right, bottom, 0x50000000);
        int divider = left + (right - left) * layout.panelPercent() / 100;
        graphics.fill(divider, top, divider + 1, bottom, 0xA0808080);

        int availableWidth = Math.max(1, divider - left - 12);
        int availableHeight = Math.max(1, bottom - top - 28);
        int gridX = left + 6;
        int gridY = top + 20;
        int count = 0;
        Component hoveredBinding = null;
        java.util.List<TalentBookLayoutDefinition.SlotDefinition> slots = previewSlots();
        if (slots.isEmpty()) {
            float scale = Math.min(1.0F, Math.min(
                    availableWidth / (float) (layout.columns() * layout.slotWidth()),
                    availableHeight / (float) (layout.rows() * layout.slotHeight())
            ));
            int slotWidth = Math.max(8, Math.round(layout.slotWidth() * scale));
            int slotHeight = Math.max(8, Math.round(layout.slotHeight() * scale));
            int iconSize = Math.max(3, Math.round(layout.iconSize() * scale));
            for (int row = 0; row < layout.rows(); row++) {
                for (int column = 0; column < layout.columns(); column++) {
                    int x = gridX + column * slotWidth;
                    int y = gridY + row * slotHeight;
                    drawSlot(graphics, x, y, slotWidth, slotHeight, iconSize, count++);
                }
            }
        } else {
            int logicalWidth = slots.stream()
                    .mapToInt(slot -> slot.x() + slot.width()).max().orElse(1);
            int logicalHeight = slots.stream()
                    .mapToInt(slot -> slot.y() + slot.height()).max().orElse(1);
            float scale = Math.min(1.0F, Math.min(
                    availableWidth / (float) logicalWidth,
                    availableHeight / (float) logicalHeight
            ));
            for (TalentBookLayoutDefinition.SlotDefinition slot : slots) {
                int x = gridX + Math.round(slot.x() * scale);
                int y = gridY + Math.round(slot.y() * scale);
                int slotWidth = Math.max(8, Math.round(slot.width() * scale));
                int slotHeight = Math.max(8, Math.round(slot.height() * scale));
                drawSlot(
                        graphics,
                        x, y, slotWidth, slotHeight,
                        Math.max(3, Math.round(slot.iconSize() * scale)),
                        count++
                );
                if (slot.talent().isPresent()) {
                    graphics.fill(x, y, x + slotWidth - 2, y + 2, 0xFFFFCC55);
                }
                if (mouseX >= x && mouseX < x + slotWidth && mouseY >= y && mouseY < y + slotHeight) {
                    hoveredBinding = slot.talent().isPresent()
                            ? Component.translatable("screen.talent_editor.layout.slot.bound", slot.talent().orElseThrow())
                            : Component.translatable("screen.talent_editor.layout.slot.auto");
                }
            }
        }
        graphics.drawString(font, font.plainSubstrByWidth(previewPageLabel(), availableWidth),
                left + 6, top + 6, 0xC0C0C0, false);
        int detailX = divider + 10;
        graphics.drawString(font, Component.translatable("screen.talent_editor.layout.preview_talent"),
                detailX, top + 16, 0x55AAFF, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.layout.preview_description"),
                detailX, top + 36, 0xD0D0D0, false);
        graphics.drawString(font, Component.translatable(layout.showGeneratedEffects()
                        ? "screen.talent_editor.layout.preview_effects_visible"
                        : "screen.talent_editor.layout.preview_effects_hidden"),
                detailX, top + 55, layout.showGeneratedEffects() ? 0x9FE59F : 0x808080, false);
        String geometry = slots.isEmpty()
                ? layout.columns() + "×" + layout.rows() + " · " + layout.slotWidth() + "×" + layout.slotHeight()
                : Component.translatable("screen.talent_editor.layout.free_slots", slots.size()).getString();
        graphics.drawString(font, layout.id() + " · " + previewPageLabel() + " · " + geometry,
                left + 4, bottom + 5, 0xA0A0A0, false);
        if (backgroundMissing) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.layout.preview_background_missing"),
                    divider + 10, bottom - 15, 0xFFAA55, false);
        }
        if (hoveredBinding != null) graphics.renderTooltip(font, hoveredBinding, mouseX, mouseY);
    }

    private static void drawSlot(
            GuiGraphics graphics, int x, int y, int width, int height, int iconSize, int index
    ) {
        int color = (index & 1) == 0 ? 0x8055AAFF : 0x80AA55FF;
        graphics.fill(x, y, x + width - 2, y + height - 2, 0x60404040);
        graphics.fill(x + 2, y + 2, x + 4, y + height - 4, color);
        int iconY = y + Math.max(2, (height - iconSize) / 2);
        graphics.fill(x + 7, iconY, x + 7 + iconSize, iconY + iconSize, color);
    }

    private java.util.List<TalentBookLayoutDefinition.SlotDefinition> previewSlots() {
        return previewPage < layout.pages().size() ? layout.pages().get(previewPage).slots() : layout.slots();
    }

    private java.util.Optional<net.minecraft.resources.ResourceLocation> previewBackground() {
        return previewPage < layout.pages().size()
                ? layout.backgroundForPage(previewPage) : layout.background();
    }

    private String previewPageLabel() {
        if (previewPage < layout.pages().size()) {
            return Component.translatable("screen.talent_editor.layout.preview_configured_page",
                    previewPage + 1, layout.pages().get(previewPage).id()).getString();
        }
        return Component.translatable("screen.talent_editor.layout.preview_fallback_page").getString();
    }

    private void changePage(int delta) {
        previewPage = Math.max(0, Math.min(previewPage + delta, layout.pages().size()));
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (previousPageButton != null) previousPageButton.active = previewPage > 0;
        if (nextPageButton != null) nextPageButton.active = previewPage < layout.pages().size();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() { returnParent(); }
}
