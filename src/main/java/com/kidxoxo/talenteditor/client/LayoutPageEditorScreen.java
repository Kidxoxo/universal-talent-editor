package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LayoutPageEditorScreen extends Screen {
    private static final int MAX_PAGES = 64;
    private static final int MAX_TOTAL_SLOTS = 4_096;
    private static final int ROW_HEIGHT = 22;
    private final LayoutEditScreen parent;
    private final List<LayoutDraft.SlotDraft> defaultSlots;
    private final ArrayList<LayoutDraft.PageDraft> pages;
    private int selected = -1;
    private int scroll;
    private EditBox idField;
    private EditBox backgroundField;
    private Button backgroundPickerButton;
    private String status = "";

    public LayoutPageEditorScreen(
            LayoutEditScreen parent,
            List<LayoutDraft.PageDraft> pages,
            List<LayoutDraft.SlotDraft> defaultSlots
    ) {
        super(Component.translatable("screen.talent_editor.layout.page_editor.title"));
        this.parent = parent;
        this.pages = new ArrayList<>(pages);
        this.defaultSlots = List.copyOf(defaultSlots);
        if (!pages.isEmpty()) selected = 0;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int leftWidth = Math.min(250, Math.max(150, formWidth / 3));
        int rightX = x + leftWidth + 12;
        int rightWidth = formWidth - leftWidth - 12;
        idField = new EditBox(font, rightX, 58, rightWidth, 20, Component.empty());
        idField.setMaxLength(64);
        addRenderableWidget(idField);
        int gap = 5;
        int chooseWidth = Math.min(72, Math.max(52, rightWidth / 4));
        backgroundField = new EditBox(font, rightX, 90, rightWidth - chooseWidth - gap, 20,
                Component.empty());
        backgroundField.setMaxLength(512);
        addRenderableWidget(backgroundField);
        backgroundPickerButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openBackgroundPicker())
                .bounds(rightX + rightWidth - chooseWidth, 90, chooseWidth, 20).build());

        int half = (rightWidth - gap) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.page.slots"),
                        button -> openSlots())
                .bounds(rightX, 122, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.page.add"),
                        button -> addPage())
                .bounds(rightX + half + gap, 122, rightWidth - half - gap, 20).build());
        int third = (rightWidth - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.page.remove"),
                        button -> removePage())
                .bounds(rightX, 148, third, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.page.up"),
                        button -> movePage(-1))
                .bounds(rightX + third + gap, 148, third, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.page.down"),
                        button -> movePage(1))
                .bounds(rightX + (third + gap) * 2, 148,
                        rightWidth - (third + gap) * 2, 20).build());

        int bottom = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnWithoutApplying())
                .bounds(x, bottom, (formWidth - gap) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.page.apply"),
                        button -> apply())
                .bounds(x + (formWidth - gap) / 2 + gap, bottom,
                        formWidth - (formWidth - gap) / 2 - gap, 20).build());
        loadSelected();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int leftWidth = Math.min(250, Math.max(150, formWidth / 3));
        int rightX = x + leftWidth + 12;
        graphics.drawCenteredString(font, title, width / 2, 9, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.layout.page.list"),
                x, 35, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.layout.page.id"),
                rightX, 47, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.layout.page.background"),
                rightX, 79, 0xA0A0A0, false);
        int rows = visibleRows();
        scroll = clamp(scroll, pages.size(), rows);
        int end = Math.min(pages.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            LayoutDraft.PageDraft page = pages.get(index);
            int y = 51 + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + leftWidth
                    && mouseY >= y - 3 && mouseY < y + 16;
            if (index == selected || hovered) {
                graphics.fill(x, y - 3, x + leftWidth, y + 16,
                        index == selected ? 0x704A90E2 : 0x404A90E2);
            }
            String label = (index + 1) + ". " + page.id() + "  [" + page.slots().size() + "]";
            graphics.drawString(font, font.plainSubstrByWidth(label, leftWidth - 8),
                    x + 4, y, index == selected ? 0xFFFFFF : 0xC0C0C0, false);
        }
        if (pages.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.layout.page.empty"),
                    x + 4, 54, 0x808080, false);
        }
        graphics.drawString(font, Component.translatable("screen.talent_editor.layout.page.help"),
                rightX, 177, 0xA0A0A0, false);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, height - 42, 0xFF7777, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int leftWidth = Math.min(250, Math.max(150, formWidth / 3));
        if (button == 0 && mouseX >= x && mouseX < x + leftWidth
                && mouseY >= 48 && mouseY < height - 40) {
            int index = scroll + (int) ((mouseY - 48) / ROW_HEIGHT);
            if (index >= 0 && index < pages.size()) {
                storeSelected();
                selected = index;
                loadSelected();
                status = "";
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, pages.size(), visibleRows());
        return true;
    }

    private int visibleRows() {
        return Math.max(1, (height - 51 - 45) / ROW_HEIGHT);
    }

    private void addPage() {
        storeSelected();
        if (pages.size() >= MAX_PAGES) {
            status = Component.translatable("screen.talent_editor.layout.page.limit").getString();
            return;
        }
        Set<String> ids = new HashSet<>();
        pages.forEach(page -> ids.add(page.id()));
        int suffix = 1;
        while (ids.contains("page_" + suffix)) suffix++;
        pages.add(new LayoutDraft.PageDraft("page_" + suffix, "", defaultSlots));
        selected = pages.size() - 1;
        loadSelected();
        status = "";
    }

    private void removePage() {
        if (selected < 0 || selected >= pages.size()) return;
        pages.remove(selected);
        selected = Math.min(selected, pages.size() - 1);
        loadSelected();
        status = "";
    }

    private void movePage(int delta) {
        storeSelected();
        int target = selected + delta;
        if (selected < 0 || target < 0 || target >= pages.size()) return;
        Collections.swap(pages, selected, target);
        selected = target;
        loadSelected();
        status = "";
    }

    private void openSlots() {
        if (selected < 0 || selected >= pages.size() || minecraft == null) return;
        storeSelected();
        LayoutDraft.PageDraft page = pages.get(selected);
        List<LayoutDraft.SlotDraft> initial = page.slots().isEmpty() ? defaultSlots : page.slots();
        minecraft.setScreen(new LayoutSlotEditorScreen(
                this, initial, defaultSlots, this::updateSelectedSlots, parent.bootstrap(), true
        ));
    }

    private void openBackgroundPicker() {
        if (selected < 0 || selected >= pages.size() || minecraft == null) return;
        storeSelected();
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.TEXTURE, parent.bootstrap(), this::chooseBackground
        ));
    }

    private void chooseBackground(ResourceLocation id) {
        if (selected < 0 || selected >= pages.size()) return;
        LayoutDraft.PageDraft page = pages.get(selected);
        pages.set(selected, new LayoutDraft.PageDraft(page.id(), id.toString(), page.slots()));
    }

    private void updateSelectedSlots(List<LayoutDraft.SlotDraft> slots) {
        if (selected < 0 || selected >= pages.size()) return;
        LayoutDraft.PageDraft page = pages.get(selected);
        pages.set(selected, new LayoutDraft.PageDraft(page.id(), page.background(), slots));
    }

    private void apply() {
        storeSelected();
        HashSet<String> ids = new HashSet<>();
        HashSet<String> fixedTalents = new HashSet<>();
        int totalSlots = 0;
        for (LayoutDraft.PageDraft page : pages) {
            if (!page.id().matches("[a-z0-9_.-]{1,64}")) {
                status = Component.translatable("screen.talent_editor.layout.page.invalid_id", page.id()).getString();
                return;
            }
            if (!ids.add(page.id())) {
                status = Component.translatable("screen.talent_editor.layout.page.duplicate_id", page.id()).getString();
                return;
            }
            if (!page.background().isBlank()) {
                ResourceLocation parsed = ResourceLocation.tryParse(page.background());
                if (parsed == null || !page.background().contains(":")) {
                    status = Component.translatable("screen.talent_editor.layout.page.invalid_background",
                            page.id()).getString();
                    return;
                }
            }
            if (page.slots().isEmpty()) {
                status = Component.translatable("screen.talent_editor.layout.page.no_slots", page.id()).getString();
                return;
            }
            totalSlots += page.slots().size();
            for (var slot : page.slots()) {
                if (!slot.talent().isBlank() && !fixedTalents.add(slot.talent())) {
                    status = Component.translatable("screen.talent_editor.layout.slot.duplicate_binding",
                            slot.talent()).getString();
                    return;
                }
            }
        }
        if (totalSlots > MAX_TOTAL_SLOTS) {
            status = Component.translatable("screen.talent_editor.layout.page.slot_limit").getString();
            return;
        }
        parent.updatePages(pages);
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void storeSelected() {
        if (selected < 0 || selected >= pages.size() || idField == null || backgroundField == null) return;
        LayoutDraft.PageDraft page = pages.get(selected);
        pages.set(selected, new LayoutDraft.PageDraft(
                idField.getValue().strip(), backgroundField.getValue().strip(), page.slots()
        ));
    }

    private void loadSelected() {
        if (idField == null || backgroundField == null) return;
        if (selected < 0 || selected >= pages.size()) {
            idField.setValue("");
            backgroundField.setValue("");
            idField.setEditable(false);
            backgroundField.setEditable(false);
            backgroundPickerButton.active = false;
        } else {
            LayoutDraft.PageDraft page = pages.get(selected);
            idField.setEditable(true);
            backgroundField.setEditable(true);
            backgroundPickerButton.active = true;
            idField.setValue(page.id());
            backgroundField.setValue(page.background());
        }
    }

    private void returnWithoutApplying() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() { returnWithoutApplying(); }

    private static int clamp(int value, int size, int rows) {
        return Math.max(0, Math.min(value, Math.max(0, size - rows)));
    }
}
