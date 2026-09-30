package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Lists only particle types that can be spawned without type-specific parameters. */
final class ParticleTypePickerScreen extends Screen {
    private static final int LIST_TOP = 66;
    private static final int ROW_HEIGHT = 18;
    private static final int SCROLLBAR_WIDTH = 7;

    private final Screen parent;
    private final ResourceLocation current;
    private final Consumer<ResourceLocation> selected;
    private final List<ResourceLocation> registered = ForgeRegistries.PARTICLE_TYPES.getKeys().stream()
            .filter(id -> ForgeRegistries.PARTICLE_TYPES.getValue(id) instanceof SimpleParticleType)
            .sorted(java.util.Comparator.comparing(ResourceLocation::toString)).toList();
    private EditBox search;
    private String query = "";
    private int scroll;
    private boolean positioned;
    private boolean draggingScrollbar;
    private boolean draggingList;
    private boolean listMoved;
    private int pressedIndex = -1;
    private double pressY;
    private int pressScroll;
    private double thumbGrabOffset;

    ParticleTypePickerScreen(Screen parent, ResourceLocation current, Consumer<ResourceLocation> selected) {
        super(Component.translatable("screen.talent_editor.particles.search"));
        this.parent = parent;
        this.current = current;
        this.selected = selected;
    }

    @Override
    protected void init() {
        int w = listWidth(), x = (width - w) / 2;
        search = new EditBox(font, x, 35, w, 20, Component.empty());
        search.setMaxLength(160);
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            scroll = 0;
        });
        addRenderableWidget(search);
        if (!positioned) {
            int index = registered.indexOf(current);
            if (index >= 0) scroll = clamp(index - visibleRows() / 2, registered.size());
            positioned = true;
        } else {
            scroll = clamp(scroll, matches().size());
        }
    }

    private int listWidth() { return Math.min(500, width - 32); }
    private int listBottom() { return height - 29; }
    private int visibleRows() { return Math.max(1, (listBottom() - LIST_TOP) / ROW_HEIGHT); }
    private int maxScroll(int size) { return Math.max(0, size - visibleRows()); }
    private int clamp(int value, int size) { return Math.max(0, Math.min(value, maxScroll(size))); }
    private int scrollbarX() { return (width - listWidth()) / 2 + listWidth() - SCROLLBAR_WIDTH; }

    private List<ResourceLocation> matches() {
        String term = query.strip().toLowerCase(Locale.ROOT);
        return registered.stream().filter(id -> id.toString().contains(term)).toList();
    }

    private int thumbHeight(int size) {
        int trackHeight = Math.max(1, listBottom() - LIST_TOP);
        return Math.min(trackHeight, Math.max(16, trackHeight * visibleRows() / Math.max(1, size)));
    }

    private int thumbTop(int size) {
        int travel = Math.max(0, listBottom() - LIST_TOP - thumbHeight(size));
        int maximum = maxScroll(size);
        return LIST_TOP + (maximum == 0 ? 0 : travel * scroll / maximum);
    }

    private void scrollToThumb(double mouseY, int size) {
        int travel = Math.max(0, listBottom() - LIST_TOP - thumbHeight(size));
        scroll = travel == 0 ? 0 : clamp((int) Math.round(
                (mouseY - thumbGrabOffset - LIST_TOP) * maxScroll(size) / travel), size);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = listWidth(), x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 7, 25, w + 14, height - 53);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        List<ResourceLocation> visible = matches();
        scroll = clamp(scroll, visible.size());
        for (int i = scroll; i < Math.min(visible.size(), scroll + visibleRows()); i++) {
            int y = LIST_TOP + (i - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < scrollbarX()
                    && mouseY >= y - 2 && mouseY < y + 15;
            boolean active = visible.get(i).equals(current);
            if (active || hovered) graphics.fill(x, y - 2, scrollbarX() - 2, y + 15,
                    active ? 0x9061B3EB : 0x4050A0E8);
            graphics.drawString(font, font.plainSubstrByWidth(visible.get(i).toString(), w - 20),
                    x + 5, y, EditorTheme.TEXT, false);
        }
        if (maxScroll(visible.size()) > 0) {
            int barX = scrollbarX();
            int top = thumbTop(visible.size());
            graphics.fill(barX, LIST_TOP, barX + SCROLLBAR_WIDTH, listBottom(), 0x8050697D);
            graphics.fill(barX, top, barX + SCROLLBAR_WIDTH, top + thumbHeight(visible.size()),
                    EditorTheme.ACCENT);
        }
        graphics.drawString(font, visible.size() + " / " + registered.size(), x, height - 23,
                EditorTheme.MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (width - listWidth()) / 2;
            List<ResourceLocation> visible = matches();
            if (mouseY >= LIST_TOP && mouseY < listBottom() && mouseX >= scrollbarX()
                    && mouseX < x + listWidth() && maxScroll(visible.size()) > 0) {
                draggingScrollbar = true;
                int top = thumbTop(visible.size());
                int bottom = top + thumbHeight(visible.size());
                thumbGrabOffset = mouseY >= top && mouseY < bottom
                        ? mouseY - top : thumbHeight(visible.size()) / 2.0;
                scrollToThumb(mouseY, visible.size());
                return true;
            }
            if (mouseY >= LIST_TOP && mouseY < listBottom() && mouseX >= x
                    && mouseX < scrollbarX()) {
                draggingList = true;
                listMoved = false;
                pressY = mouseY;
                pressScroll = scroll;
                pressedIndex = scroll + (int) ((mouseY - LIST_TOP) / ROW_HEIGHT);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollbar) {
            scrollToThumb(mouseY, matches().size());
            return true;
        }
        if (button == 0 && draggingList) {
            if (Math.abs(mouseY - pressY) > 4) listMoved = true;
            if (listMoved) scroll = clamp(
                    pressScroll + (int) Math.round((pressY - mouseY) / ROW_HEIGHT), matches().size());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        if (button == 0 && draggingList) {
            draggingList = false;
            List<ResourceLocation> visible = matches();
            int x = (width - listWidth()) / 2;
            int releaseIndex = scroll + (int) ((mouseY - LIST_TOP) / ROW_HEIGHT);
            if (!listMoved && mouseX >= x && mouseX < scrollbarX()
                    && mouseY >= LIST_TOP && mouseY < listBottom()
                    && releaseIndex == pressedIndex && releaseIndex < visible.size()) {
                selected.accept(visible.get(releaseIndex));
                onClose();
            }
            pressedIndex = -1;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, matches().size());
        return true;
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
