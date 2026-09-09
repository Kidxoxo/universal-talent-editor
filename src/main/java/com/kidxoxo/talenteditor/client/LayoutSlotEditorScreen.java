package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.kidxoxo.talenteditor.network.EditorBootstrap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LayoutSlotEditorScreen extends Screen {
    private static final int BASE_LOGICAL_WIDTH = 240;
    private static final int BASE_LOGICAL_HEIGHT = 140;
    private static final int MAX_SLOTS = 256;
    private final Screen parent;
    private final EditorBootstrap bootstrap;
    private final boolean allowFixedTalent;
    private Button bindButton;
    private Button clearBindingButton;
    private final java.util.function.Consumer<List<LayoutDraft.SlotDraft>> applyHandler;
    private final List<LayoutDraft.SlotDraft> gridTemplate;
    private final ArrayList<LayoutDraft.SlotDraft> slots;
    private int selected = -1;
    private DragMode dragMode = DragMode.NONE;
    private int dragOffsetX;
    private int dragOffsetY;
    private String status = "";

    public LayoutSlotEditorScreen(
            LayoutEditScreen parent,
            List<LayoutDraft.SlotDraft> slots,
            List<LayoutDraft.SlotDraft> gridTemplate
    ) {
        this(parent, slots, gridTemplate, parent::updateSlots, parent.bootstrap(), false);
    }

    public LayoutSlotEditorScreen(
            Screen parent,
            List<LayoutDraft.SlotDraft> slots,
            List<LayoutDraft.SlotDraft> gridTemplate,
            java.util.function.Consumer<List<LayoutDraft.SlotDraft>> applyHandler,
            EditorBootstrap bootstrap,
            boolean allowFixedTalent
    ) {
        super(Component.translatable("screen.talent_editor.layout.slot_editor.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.allowFixedTalent = allowFixedTalent;
        this.applyHandler = applyHandler;
        this.slots = new ArrayList<>(slots);
        this.gridTemplate = List.copyOf(gridTemplate);
        if (!slots.isEmpty()) selected = 0;
    }

    @Override
    protected void init() {
        dragMode = DragMode.NONE;
        int gap = 5;
        int formWidth = Math.min(520, width - 32);
        int x = (width - formWidth) / 2;
        int buttonWidth = Math.max(42, (formWidth - gap * 4) / 5);
        int y = height - 28;
        if (allowFixedTalent) {
            bindButton = addRenderableWidget(Button.builder(
                            Component.translatable("screen.talent_editor.layout.slot.bind"), button -> bindTalent())
                    .bounds(x, 34, (formWidth - gap) / 2, 20).build());
            clearBindingButton = addRenderableWidget(Button.builder(
                            Component.translatable("screen.talent_editor.layout.slot.clear_binding"),
                            button -> setTalent(""))
                    .bounds(x + (formWidth - gap) / 2 + gap, 34, formWidth / 2 - gap / 2, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnWithoutApplying())
                .bounds(x, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.slot.add"),
                        button -> addSlot())
                .bounds(x + buttonWidth + gap, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.slot.from_grid"),
                        button -> importGrid())
                .bounds(x + (buttonWidth + gap) * 2, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.slot.remove"),
                        button -> removeSelected())
                .bounds(x + (buttonWidth + gap) * 3, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.slot.apply"),
                        button -> apply())
                .bounds(x + (buttonWidth + gap) * 4, y,
                        formWidth - (buttonWidth + gap) * 4, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (bindButton != null) bindButton.active = selected >= 0 && selected < slots.size();
        if (clearBindingButton != null) clearBindingButton.active = selected >= 0 && selected < slots.size()
                && !slots.get(selected).talent().isBlank();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.layout.slot_editor.help"),
                width / 2, 20, 0xA0A0A0);
        Canvas canvas = canvas();
        graphics.fill(canvas.left(), canvas.top(), canvas.right(), canvas.bottom(), 0x70000000);
        boolean overlap = hasOverlap();
        for (int index = 0; index < slots.size(); index++) {
            LayoutDraft.SlotDraft slot = slots.get(index);
            Rect rect = rect(slot, canvas);
            boolean hovered = rect.contains(mouseX, mouseY);
            int border = index == selected ? 0xFF55AAFF : hovered ? 0xFFAAAAAA : 0xFF606060;
            if (overlap && overlapsAny(index)) border = 0xFFFF5555;
            graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), border);
            graphics.fill(rect.left() + 1, rect.top() + 1, rect.right() - 1, rect.bottom() - 1, 0xC0303030);
            int icon = Math.max(3, Math.round(slot.iconSize() * canvas.scale()));
            int iconY = rect.top() + Math.max(2, (rect.height() - icon) / 2);
            graphics.fill(rect.left() + 5, iconY, rect.left() + 5 + icon, iconY + icon, 0xFF8A55CC);
            if (rect.width() > icon + 20) {
                String label = slot.talent().isBlank() ? slot.id() : "* " + slot.talent();
                graphics.drawString(font, font.plainSubstrByWidth(label, rect.width() - icon - 12),
                        rect.left() + icon + 8, rect.top() + 4, 0xE8E8E8, false);
            }
            graphics.fill(rect.right() - 5, rect.bottom() - 5, rect.right(), rect.bottom(), 0xFFFFFFFF);
        }
        String footer = status;
        if (footer.isBlank() && selected >= 0 && selected < slots.size()) {
            LayoutDraft.SlotDraft slot = slots.get(selected);
            footer = slot.id() + "  x=" + slot.x() + " y=" + slot.y()
                    + "  " + slot.width() + "×" + slot.height() + "  icon=" + slot.iconSize()
                    + "  " + Component.translatable(slot.talent().isBlank()
                    ? "screen.talent_editor.layout.slot.auto" : "screen.talent_editor.layout.slot.bound",
                    slot.talent()).getString();
        }
        if (footer.isBlank()) footer = Component.translatable("screen.talent_editor.layout.slot.grid_mode").getString();
        graphics.drawString(font, font.plainSubstrByWidth(footer, width - 32),
                16, height - 43, overlap ? 0xFF5555 : 0xA0A0A0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Canvas canvas = canvas();
        if (canvas.contains(mouseX, mouseY)) {
            for (int index = slots.size() - 1; index >= 0; index--) {
                LayoutDraft.SlotDraft slot = slots.get(index);
                Rect rect = rect(slot, canvas);
                if (!rect.contains(mouseX, mouseY)) continue;
                selected = index;
                int logicalX = canvas.logicalX(mouseX);
                int logicalY = canvas.logicalY(mouseY);
                if (button == 1 || mouseX >= rect.right() - 7 && mouseY >= rect.bottom() - 7) {
                    dragMode = DragMode.RESIZE;
                } else if (button == 0) {
                    dragMode = DragMode.MOVE;
                    dragOffsetX = logicalX - slot.x();
                    dragOffsetY = logicalY - slot.y();
                }
                status = "";
                return true;
            }
            selected = -1;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (selected < 0 || selected >= slots.size() || dragMode == DragMode.NONE) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        Canvas canvas = canvas();
        LayoutDraft.SlotDraft slot = slots.get(selected);
        int logicalX = canvas.logicalX(mouseX);
        int logicalY = canvas.logicalY(mouseY);
        if (dragMode == DragMode.MOVE) {
            int x = clamp(logicalX - dragOffsetX, 0, Math.max(0, canvas.logicalWidth() - slot.width()));
            int y = clamp(logicalY - dragOffsetY, 0, Math.max(0, canvas.logicalHeight() - slot.height()));
            slots.set(selected, slot.withGeometry(x, y, slot.width(), slot.height(), slot.iconSize()));
        } else {
            int width = clamp(logicalX - slot.x(), 24, Math.min(256, canvas.logicalWidth() - slot.x()));
            int height = clamp(logicalY - slot.y(), 22, Math.min(128, canvas.logicalHeight() - slot.y()));
            int iconSize = Math.min(slot.iconSize(), Math.max(8, height - 4));
            slots.set(selected, slot.withGeometry(slot.x(), slot.y(), width, height, iconSize));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragMode = DragMode.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void addSlot() {
        if (slots.size() >= MAX_SLOTS) {
            status = Component.translatable("screen.talent_editor.layout.slot.limit").getString();
            return;
        }
        Set<String> ids = new HashSet<>();
        slots.forEach(slot -> ids.add(slot.id()));
        int suffix = 1;
        while (ids.contains("slot_" + suffix)) suffix++;
        int offset = (slots.size() * 8) % 80;
        slots.add(new LayoutDraft.SlotDraft("slot_" + suffix, offset, offset, 58, 30, 16));
        selected = slots.size() - 1;
        status = "";
    }

    private void importGrid() {
        slots.clear();
        slots.addAll(gridTemplate.stream().limit(MAX_SLOTS).toList());
        selected = slots.isEmpty() ? -1 : 0;
        status = "";
    }

    private void removeSelected() {
        if (selected < 0 || selected >= slots.size()) return;
        slots.remove(selected);
        selected = Math.min(selected, slots.size() - 1);
        status = "";
    }

    private void apply() {
        if (hasOverlap()) {
            status = Component.translatable("screen.talent_editor.layout.slot.overlap").getString();
            return;
        }
        Set<String> fixed = new HashSet<>();
        for (var slot : slots) {
            if (!slot.talent().isBlank() && !fixed.add(slot.talent())) {
                status = Component.translatable("screen.talent_editor.layout.slot.duplicate_binding",
                        slot.talent()).getString();
                return;
            }
        }
        applyHandler.accept(List.copyOf(slots));
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void returnWithoutApplying() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void bindTalent() {
        if (!allowFixedTalent || minecraft == null || selected < 0 || selected >= slots.size()) return;
        dragMode = DragMode.NONE;
        minecraft.setScreen(new ResourceReferencePickerScreen(this,
                ResourceReferencePickerScreen.Source.TALENT, bootstrap, id -> setTalent(id.toString())));
    }

    private void setTalent(String talent) {
        if (!allowFixedTalent || selected < 0 || selected >= slots.size()) return;
        slots.set(selected, slots.get(selected).withTalent(talent));
        status = "";
    }

    @Override
    public void onClose() { returnWithoutApplying(); }

    private Canvas canvas() {
        int logicalWidth = BASE_LOGICAL_WIDTH;
        int logicalHeight = BASE_LOGICAL_HEIGHT;
        for (LayoutDraft.SlotDraft slot : slots) {
            logicalWidth = Math.max(logicalWidth, slot.x() + slot.width() + 8);
            logicalHeight = Math.max(logicalHeight, slot.y() + slot.height() + 8);
        }
        int left = 16;
        int top = allowFixedTalent ? 60 : 34;
        int right = width - 16;
        int bottom = height - 58;
        float scale = Math.max(0.01F, Math.min(
                (right - left) / (float) logicalWidth,
                (bottom - top) / (float) logicalHeight
        ));
        return new Canvas(left, top, right, bottom, logicalWidth, logicalHeight, scale);
    }

    private static Rect rect(LayoutDraft.SlotDraft slot, Canvas canvas) {
        int left = canvas.left() + Math.round(slot.x() * canvas.scale());
        int top = canvas.top() + Math.round(slot.y() * canvas.scale());
        int right = left + Math.max(8, Math.round(slot.width() * canvas.scale()));
        int bottom = top + Math.max(8, Math.round(slot.height() * canvas.scale()));
        return new Rect(left, top, right, bottom);
    }

    private boolean hasOverlap() {
        for (int first = 0; first < slots.size(); first++) {
            if (overlapsAny(first)) return true;
        }
        return false;
    }

    private boolean overlapsAny(int index) {
        LayoutDraft.SlotDraft first = slots.get(index);
        for (int other = 0; other < slots.size(); other++) {
            if (other == index) continue;
            LayoutDraft.SlotDraft second = slots.get(other);
            if (first.x() < second.x() + second.width()
                    && first.x() + first.width() > second.x()
                    && first.y() < second.y() + second.height()
                    && first.y() + first.height() > second.y()) return true;
        }
        return false;
    }

    private static int clamp(int value, int minimum, int maximum) {
        if (maximum < minimum) return minimum;
        return Math.max(minimum, Math.min(value, maximum));
    }

    private enum DragMode { NONE, MOVE, RESIZE }

    private record Canvas(
            int left, int top, int right, int bottom,
            int logicalWidth, int logicalHeight, float scale
    ) {
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
        int logicalX(double x) { return Math.round((float) (x - left) / scale); }
        int logicalY(double y) { return Math.round((float) (y - top) / scale); }
    }

    private record Rect(int left, int top, int right, int bottom) {
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
        int width() { return right - left; }
        int height() { return bottom - top; }
    }
}
