package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.network.PlayerTalentState;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

public final class TalentBookScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
    private static final ResourceLocation DEFAULT_POOL = new ResourceLocation("talent_editor", "default");
    private PlayerTalentState state;
    private TalentBookLayoutDefinition layout;
    private Button drawButton;
    private Button modeButton;
    private Button synthesizeButton;
    private Button closeButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private BookMode mode = BookMode.OWNED;
    private int selectedIndex;
    private int scroll;
    private int page;
    private TalentBookPagination.Placement placement;

    public TalentBookScreen(PlayerTalentState state) {
        this(state, TalentBookLayoutDefinition.DEFAULT);
    }

    public TalentBookScreen(PlayerTalentState state, TalentBookLayoutDefinition layout) {
        super(Component.translatable("screen.talent_editor.book.title"));
        this.state = state;
        this.layout = layout;
        rebuildPlacement();
        selectedIndex = placement.firstIndex(0);
    }

    public void updateState(PlayerTalentState state) {
        updateState(state, layout);
    }

    public void updateState(PlayerTalentState state, TalentBookLayoutDefinition layout) {
        ResourceLocation selectedId = selectedEntryId();
        int previousPage = page;
        this.state = state;
        this.layout = layout;
        rebuildPlacement();
        if (mode == BookMode.COUNTERS) {
            selectedIndex = findEntryIndex(selectedId);
        } else if (selectedId != null && findIndex(visibleTalents(), selectedId) >= 0) {
            selectedIndex = findEntryIndex(selectedId);
            page = pageForTalentIndex(selectedIndex);
        } else {
            page = Math.max(0, Math.min(previousPage, talentPageCount() - 1));
            selectedIndex = placement.firstIndex(page);
        }
        updateButtons();
    }

    @Override
    protected void init() {
        normalizeModeForSettings();
        rebuildPlacement();
        int selectedPage = placement.pageFor(selectedIndex);
        page = mode != BookMode.COUNTERS && selectedPage >= 0 ? selectedPage
                : Math.max(0, Math.min(page, talentPageCount() - 1));
        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> toggleMode())
                .bounds(8, height - 28, 1, 20).build());
        synthesizeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.book.synthesize_manual"),
                        button -> synthesizeSelected())
                .bounds(8, height - 28, 1, 20).build());
        drawButton = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.book.draw"),
                        button -> EditorNetwork.requestDraw(DEFAULT_POOL))
                .bounds(8, height - 28, 1, 20).build());
        closeButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(8, height - 28, 1, 20).build());
        previousPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(8, 20, 20, 20).build());
        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(32, 20, 20, 20).build());
        layoutBottomButtons();
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (mode != BookMode.COUNTERS) {
            page = Math.max(0, Math.min(page, talentPageCount() - 1));
        }
        renderBackground(graphics);
        layout.backgroundForPage(page).flatMap(id -> minecraft.getResourceManager().getResource(id)).ifPresent(resource ->
                graphics.blit(layout.backgroundForPage(page).orElseThrow(), 0, 0, 0.0F, 0.0F,
                        width, height, width, height)
        );
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(
                "screen.talent_editor.book.summary_extended", state.talents().size(), state.syntheses().size(),
                state.counters().size(), state.drawTokens(), state.definitionRevision()
        ), width / 2, 25, 0xA0A0A0);
        if (mode != BookMode.COUNTERS) {
            graphics.drawString(font, Component.translatable(
                    "screen.talent_editor.book.page", page + 1, talentPageCount()
            ), 58, 26, 0xA0A0A0, false);
        }
        int divider = divider();
        graphics.fill(divider, 42, divider + 1, height - 36, 0x60808080);
        renderTalentList(graphics, mouseX, mouseY, divider);
        renderDetails(graphics, divider);
        if (state.truncated() || state.synthesesTruncated() || state.countersTruncated()) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.book.truncated_any"),
                    12, height - 24, 0xFFAA55, false);
        }
    }

    private void renderTalentList(GuiGraphics graphics, int mouseX, int mouseY, int divider) {
        if (mode == BookMode.COUNTERS) {
            renderCounterList(graphics, mouseX, mouseY, divider);
            return;
        }
        List<PlayerTalentState.TalentView> talents = visibleTalents();
        page = Math.max(0, Math.min(page, talentPageCount() - 1));
        int capacity = talentPageCapacity();
        for (int local = 0; local < capacity; local++) {
            int index = placement.indexAt(page, local);
            SlotBounds bounds = slotBounds(local);
            if (index < 0) {
                if (!activeSlots().isEmpty() && activeSlots().get(local).talent().isPresent()) {
                    graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), 0x30303030);
                    graphics.drawString(font, font.plainSubstrByWidth(
                            Component.translatable("screen.talent_editor.book.reserved_slot").getString(),
                            Math.max(0, bounds.right() - bounds.x() - 6)),
                            bounds.x() + 3, bounds.y() + 3, 0x808080, false);
                }
                continue;
            }
            PlayerTalentState.TalentView talent = talents.get(index);
            int x = bounds.x();
            int y = bounds.y();
            int right = bounds.right();
            int bottom = bounds.bottom();
            boolean selected = selectedIndex == index;
            boolean hovered = mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom;
            int rarity = 0xFF000000 | (talent.rarityColor() & 0xFFFFFF);
            if (selected || hovered) graphics.fill(x, y, right, bottom,
                    selected ? 0x704A90E2 : 0x404A90E2);
            graphics.fill(x + 2, y + 2, x + 4, bottom - 2, rarity);
            int nameColor = talent.available() ? rarity : 0xFF808080;
            int textX = x + 7;
            if (talent.icon().isPresent()) {
                int iconY = y + Math.max(2, (bounds.height() - bounds.iconSize()) / 2);
                renderIcon(graphics, talent.icon().orElseThrow(), x + 7, iconY, bounds.iconSize());
                textX = x + 11 + bounds.iconSize();
            }
            int textWidth = Math.max(12, right - textX - 3);
            graphics.drawString(font, fit(talent.name(), textWidth, nameColor),
                    textX, y + 3, nameColor, false);
            String subline = talent.id().toString();
            int sublineColor = 0x909090;
            if (mode == BookMode.SYNTHESIS) {
                PlayerTalentState.SynthesisView synthesis = synthesisAt(index);
                subline = Component.translatable(synthesis.ready()
                        ? "screen.talent_editor.book.synthesis_ready"
                        : "screen.talent_editor.book.synthesis_blocked").getString() + " · " + subline;
                sublineColor = synthesis.ready() ? 0x55FF55 : 0xFFAA55;
            }
            if (bounds.height() >= 27) {
                graphics.drawString(font, fit(subline, textWidth), textX, y + 14, sublineColor, false);
            }
        }
        if (talents.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(mode == BookMode.SYNTHESIS
                            ? "screen.talent_editor.book.no_syntheses" : "screen.talent_editor.book.empty"),
                    divider / 2, height / 2, 0xA0A0A0);
        }
    }

    private void renderDetails(GuiGraphics graphics, int divider) {
        if (mode == BookMode.COUNTERS) {
            renderCounterDetails(graphics, divider);
            return;
        }
        List<PlayerTalentState.TalentView> talents = visibleTalents();
        if (talents.isEmpty() || selectedIndex < 0 || selectedIndex >= talents.size()) return;
        PlayerTalentState.TalentView talent = talents.get(selectedIndex);
        int x = divider + 13;
        int contentWidth = Math.max(40, width - x - 14);
        int rarityColor = 0xFF000000 | (talent.rarityColor() & 0xFFFFFF);
        int headingX = x;
        int headingWidth = contentWidth;
        if (talent.icon().isPresent()) {
            graphics.blit(talent.icon().orElseThrow(), x, 50, 0.0F, 0.0F, 16, 16, 16, 16);
            headingX += 22;
            headingWidth -= 22;
        }
        graphics.drawString(font, fit(talent.name(), headingWidth, rarityColor),
                headingX, 50, rarityColor, false);
        graphics.drawString(font, fit(talent.rarityName().component(0xA0A0A0).getString()
                        + " · " + talent.id(), headingWidth),
                headingX, 64, 0xA0A0A0, false);
        int y = 84;
        if (!talent.defined()) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.book.missing_definition"),
                    x, y, 0xFF7777, false);
            return;
        }
        if (!talent.available()) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.book.unavailable"),
                    x, y, 0xFFAA55, false);
            y += 15;
        }
        for (PlayerTalentState.StyledTextView paragraph : talent.description()) {
            List<FormattedCharSequence> lines = font.split(paragraph.component(0xD0D0D0), contentWidth);
            for (FormattedCharSequence line : lines) {
                if (y > height - 48) return;
                graphics.drawString(font, line, x, y, 0xD0D0D0, false);
                y += 11;
            }
            y += 3;
        }
        if (!layout.showGeneratedEffects() || !talent.showGeneratedEffects()) {
            if (y <= height - 48) {
                graphics.drawString(font, Component.translatable("screen.talent_editor.book.effects_hidden"),
                        x, y, 0x808080, false);
                y += 14;
            }
        } else {
            List<PlayerTalentState.AttributeEffectView> visibleEffects = talent.attributeEffects().stream()
                    .filter(PlayerTalentState.AttributeEffectView::showInDescription)
                    .toList();
            if (!visibleEffects.isEmpty() && y <= height - 48) {
                graphics.drawString(font, Component.translatable("screen.talent_editor.book.effects"),
                        x, y, 0xFFFFFF, false);
                y += 14;
            }
            for (PlayerTalentState.AttributeEffectView effect : visibleEffects) {
                if (y > height - 48) break;
                String value = effect.formula().isBlank() ? signed(effect.amount()) : effect.formula();
                String line = effect.attribute() + "  " + value + "  "
                        + Component.translatable("screen.talent_editor.operation." + effect.operation()).getString();
                graphics.drawString(font, fit(line, contentWidth), x, y, 0x9FE59F, false);
                y += 12;
            }
        }
        if (mode == BookMode.SYNTHESIS) renderSynthesisCosts(graphics, synthesisAt(selectedIndex), x, y, contentWidth);
    }

    private void renderSynthesisCosts(
            GuiGraphics graphics,
            PlayerTalentState.SynthesisView synthesis,
            int x,
            int y,
            int contentWidth
    ) {
        if (y <= height - 48) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.book.synthesis_costs"),
                    x, y, 0xFFFFFF, false);
            y += 14;
        }
        if (!synthesis.conditionMet() && y <= height - 48) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.book.other_requirements_missing"),
                    x, y, 0xFF7777, false);
            y += 12;
        }
        for (PlayerTalentState.SynthesisCostView cost : synthesis.costs()) {
            if (y > height - 48) break;
            String type = Component.translatable("screen.talent_editor.book.cost_type."
                    + cost.type().name().toLowerCase(Locale.ROOT)).getString();
            String line = type + " · " + cost.id() + "  " + cost.current() + "/" + cost.required();
            graphics.drawString(font, fit(line, contentWidth), x, y,
                    cost.satisfied() ? 0x9FE59F : 0xFF7777, false);
            y += 12;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= 9 && mouseX < divider() - 7 && mouseY >= 49 && mouseY < height - 39) {
            int index;
            if (mode == BookMode.COUNTERS) {
                index = scroll + (int) ((mouseY - 49) / ROW_HEIGHT);
            } else if (!activeSlots().isEmpty()) {
                int local = -1;
                for (int slot = 0; slot < activeSlots().size(); slot++) {
                    if (slotBounds(slot).contains(mouseX, mouseY)) {
                        local = slot;
                        break;
                    }
                }
                if (local < 0) return super.mouseClicked(mouseX, mouseY, button);
                index = placement.indexAt(page, local);
            } else {
                int column = (int) ((mouseX - 9) / layout.slotWidth());
                int row = (int) ((mouseY - 49) / layout.slotHeight());
                if (column >= talentColumns() || row >= talentRows()) return super.mouseClicked(mouseX, mouseY, button);
                index = placement.indexAt(page, row * talentColumns() + column);
            }
            if (index < 0 && mode != BookMode.COUNTERS) {
                selectedIndex = -1;
                updateButtons();
                return true;
            }
            if (index >= 0 && index < visibleSize()) {
                selectedIndex = index;
                updateButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < divider()) {
            if (mode != BookMode.COUNTERS) {
                changePage(delta > 0 ? -1 : 1);
                return true;
            }
            int visibleRows = Math.max(1, (height - 49 - 39) / ROW_HEIGHT);
            scroll = clampScroll(scroll - (int) Math.signum(delta) * 3, visibleSize(), visibleRows);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int divider() {
        return Math.max(145, width * layout.panelPercent() / 100);
    }

    private int clampScroll(int value, int size, int visibleRows) {
        return Math.max(0, Math.min(value, Math.max(0, size - visibleRows)));
    }

    private void toggleMode() {
        mode = nextMode(mode, ClientEditorState.editorSettings().automaticSynthesis());
        selectedIndex = 0;
        scroll = 0;
        page = 0;
        rebuildPlacement();
        selectedIndex = mode == BookMode.COUNTERS ? 0 : placement.firstIndex(page);
        updateButtons();
    }

    private void synthesizeSelected() {
        PlayerTalentState.SynthesisView synthesis = currentSynthesis();
        if (synthesis != null) EditorNetwork.requestSynthesis(synthesis.target().id());
    }

    private void updateButtons() {
        if (drawButton != null) drawButton.active = state.drawTokens() > 0;
        if (modeButton != null) modeButton.setMessage(modeLabel());
        if (synthesizeButton != null) {
            synthesizeButton.visible = synthesisControlsVisible(
                    ClientEditorState.editorSettings().automaticSynthesis());
            synthesizeButton.active = synthesizeButton.visible
                    && mode == BookMode.SYNTHESIS && currentSynthesis() != null;
        }
        if (previousPageButton != null) {
            previousPageButton.visible = mode != BookMode.COUNTERS;
            previousPageButton.active = page > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.visible = mode != BookMode.COUNTERS;
            nextPageButton.active = page + 1 < talentPageCount();
        }
    }

    private Component modeLabel() {
        return switch (mode) {
            case OWNED -> ClientEditorState.editorSettings().automaticSynthesis()
                    ? Component.translatable("screen.talent_editor.book.show_counters", state.counters().size())
                    : Component.translatable("screen.talent_editor.book.show_syntheses_manual", state.syntheses().size());
            case SYNTHESIS -> Component.translatable("screen.talent_editor.book.show_counters", state.counters().size());
            case COUNTERS -> Component.translatable("screen.talent_editor.book.show_owned");
        };
    }

    public void refreshSettings() {
        normalizeModeForSettings();
        layoutBottomButtons();
        updateButtons();
    }

    private void normalizeModeForSettings() {
        if (!ClientEditorState.editorSettings().automaticSynthesis() || mode != BookMode.SYNTHESIS) return;
        mode = BookMode.OWNED;
        selectedIndex = 0;
        scroll = 0;
        page = 0;
        rebuildPlacement();
        selectedIndex = placement.firstIndex(page);
    }

    private void layoutBottomButtons() {
        if (modeButton == null || synthesizeButton == null || drawButton == null || closeButton == null) return;
        boolean showSynthesis = synthesisControlsVisible(
                ClientEditorState.editorSettings().automaticSynthesis());
        List<Button> buttons = showSynthesis
                ? List.of(modeButton, synthesizeButton, drawButton, closeButton)
                : List.of(modeButton, drawButton, closeButton);
        int gap = 6;
        int availableWidth = Math.max(buttons.size(), width - 16 - gap * (buttons.size() - 1));
        int buttonWidth = availableWidth / buttons.size();
        int remainder = availableWidth % buttons.size();
        int x = 8;
        for (int index = 0; index < buttons.size(); index++) {
            Button button = buttons.get(index);
            int allocatedWidth = buttonWidth + (index < remainder ? 1 : 0);
            button.setX(x);
            button.setY(height - 28);
            button.setWidth(allocatedWidth);
            x += allocatedWidth + gap;
        }
    }

    static boolean synthesisControlsVisible(boolean automaticSynthesis) {
        return !automaticSynthesis;
    }

    static BookMode nextMode(BookMode current, boolean automaticSynthesis) {
        if (automaticSynthesis) {
            return current == BookMode.COUNTERS ? BookMode.OWNED : BookMode.COUNTERS;
        }
        return switch (current) {
            case OWNED -> BookMode.SYNTHESIS;
            case SYNTHESIS -> BookMode.COUNTERS;
            case COUNTERS -> BookMode.OWNED;
        };
    }

    private List<PlayerTalentState.TalentView> visibleTalents() {
        return mode == BookMode.SYNTHESIS
                ? state.syntheses().stream().map(PlayerTalentState.SynthesisView::target).toList()
                : state.talents();
    }

    private PlayerTalentState.TalentView selectedTalent() {
        List<PlayerTalentState.TalentView> talents = visibleTalents();
        return selectedIndex >= 0 && selectedIndex < talents.size() ? talents.get(selectedIndex) : null;
    }

    private PlayerTalentState.SynthesisView currentSynthesis() {
        return mode == BookMode.SYNTHESIS ? synthesisAt(selectedIndex) : null;
    }

    private PlayerTalentState.SynthesisView synthesisAt(int index) {
        return index >= 0 && index < state.syntheses().size() ? state.syntheses().get(index) : null;
    }

    private static int findIndex(List<PlayerTalentState.TalentView> talents, ResourceLocation id) {
        if (id != null) {
            for (int index = 0; index < talents.size(); index++) {
                if (talents.get(index).id().equals(id)) return index;
            }
        }
        return -1;
    }

    private void renderCounterList(GuiGraphics graphics, int mouseX, int mouseY, int divider) {
        List<PlayerTalentState.CounterProgressView> counters = state.counters();
        int top = 49;
        int visibleRows = Math.max(1, (height - top - 39) / ROW_HEIGHT);
        scroll = clampScroll(scroll, counters.size(), visibleRows);
        int end = Math.min(counters.size(), scroll + visibleRows);
        for (int index = scroll; index < end; index++) {
            PlayerTalentState.CounterProgressView counter = counters.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean selected = selectedIndex == index;
            boolean hovered = mouseX >= 9 && mouseX < divider - 7 && mouseY >= y - 3 && mouseY < y + 28;
            if (selected || hovered) graphics.fill(9, y - 3, divider - 7, y + 28,
                    selected ? 0x704A90E2 : 0x404A90E2);
            int color = counter.active() ? 0xFF55FF55 : counter.defined() ? 0xFFFFAA55 : 0xFF808080;
            graphics.fill(12, y, 15, y + 22, color);
            graphics.drawString(font, fit(counter.name(), divider - 32, color), 20, y, color, false);
            String value = counter.maximum() > 0L
                    ? counter.current() + " / " + counter.maximum() : Long.toString(counter.current());
            graphics.drawString(font, fit(value + " · " + counter.id(), divider - 32),
                    20, y + 12, 0xA0A0A0, false);
        }
        if (counters.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.book.no_counters"),
                    divider / 2, height / 2, 0xA0A0A0);
        }
    }

    private void renderCounterDetails(GuiGraphics graphics, int divider) {
        if (selectedIndex < 0 || selectedIndex >= state.counters().size()) return;
        PlayerTalentState.CounterProgressView counter = state.counters().get(selectedIndex);
        int x = divider + 13;
        int contentWidth = Math.max(40, width - x - 14);
        int color = counter.active() ? 0xFF55FF55 : counter.defined() ? 0xFFFFAA55 : 0xFF808080;
        graphics.drawString(font, fit(counter.name(), contentWidth, color), x, 50, color, false);
        graphics.drawString(font, fit(counter.id().toString(), contentWidth), x, 64, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.book.counter_progress",
                counter.current(), counter.maximum()), x, 88, 0xFFFFFF, false);
        if (!counter.event().isBlank()) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.book.counter_event",
                    Component.translatable("screen.talent_editor.counter.event." + counter.event())),
                    x, 104, 0xA0A0A0, false);
        }
        String status = !counter.defined() ? "missing" : counter.active() ? "active" : "inactive";
        graphics.drawString(font, Component.translatable("screen.talent_editor.book.counter_status." + status),
                x, 120, color, false);
    }

    private int visibleSize() {
        return mode == BookMode.COUNTERS ? state.counters().size() : visibleTalents().size();
    }

    private ResourceLocation selectedEntryId() {
        if (mode == BookMode.COUNTERS) {
            return selectedIndex >= 0 && selectedIndex < state.counters().size()
                    ? state.counters().get(selectedIndex).id() : null;
        }
        PlayerTalentState.TalentView talent = selectedTalent();
        return talent == null ? null : talent.id();
    }

    private int findEntryIndex(ResourceLocation id) {
        if (mode != BookMode.COUNTERS) return findIndex(visibleTalents(), id);
        if (id != null) {
            for (int index = 0; index < state.counters().size(); index++) {
                if (state.counters().get(index).id().equals(id)) return index;
            }
        }
        return 0;
    }

    private int talentColumns() {
        int fitting = Math.max(1, (divider() - 16) / layout.slotWidth());
        return Math.max(1, Math.min(layout.columns(), fitting));
    }

    private int talentRows() {
        int fitting = Math.max(1, (height - 49 - 39) / layout.slotHeight());
        return Math.max(1, Math.min(layout.rows(), fitting));
    }

    private int talentPageCapacity() {
        return capacityForPage(page);
    }

    private int talentPageCount() {
        return placement.pages().size();
    }

    private void changePage(int delta) {
        int next = Math.max(0, Math.min(page + delta, talentPageCount() - 1));
        if (next == page) return;
        page = next;
        selectedIndex = placement.firstIndex(page);
        updateButtons();
    }

    private SlotBounds slotBounds(int localIndex) {
        List<TalentBookLayoutDefinition.SlotDefinition> slots = activeSlots();
        if (slots.isEmpty()) {
            int columns = talentColumns();
            int column = localIndex % columns;
            int row = localIndex / columns;
            int x = 9 + column * layout.slotWidth();
            int y = 49 + row * layout.slotHeight();
            int right = Math.min(divider() - 7, x + layout.slotWidth() - 3);
            int bottom = Math.min(height - 39, y + layout.slotHeight() - 3);
            return new SlotBounds(x, y, right, bottom, layout.iconSize());
        }
        TalentBookLayoutDefinition.SlotDefinition slot = slots.get(localIndex);
        float scale = freeSlotScale(slots);
        int x = 9 + Math.round(slot.x() * scale);
        int y = 49 + Math.round(slot.y() * scale);
        int slotWidth = Math.max(8, Math.round(slot.width() * scale));
        int slotHeight = Math.max(8, Math.round(slot.height() * scale));
        int right = Math.min(divider() - 7, x + slotWidth);
        int bottom = Math.min(height - 39, y + slotHeight);
        int iconSize = Math.max(3, Math.round(slot.iconSize() * scale));
        return new SlotBounds(x, y, right, bottom, iconSize);
    }

    private float freeSlotScale(List<TalentBookLayoutDefinition.SlotDefinition> slots) {
        int logicalWidth = 1;
        int logicalHeight = 1;
        for (TalentBookLayoutDefinition.SlotDefinition slot : slots) {
            logicalWidth = Math.max(logicalWidth, slot.x() + slot.width());
            logicalHeight = Math.max(logicalHeight, slot.y() + slot.height());
        }
        int availableWidth = Math.max(1, divider() - 16);
        int availableHeight = Math.max(1, height - 49 - 39);
        return Math.min(1.0F, Math.min(
                availableWidth / (float) logicalWidth,
                availableHeight / (float) logicalHeight
        ));
    }

    private List<TalentBookLayoutDefinition.SlotDefinition> activeSlots() {
        return layout.slotsForPage(page);
    }

    private int capacityForPage(int pageIndex) {
        List<TalentBookLayoutDefinition.SlotDefinition> slots = layout.slotsForPage(pageIndex);
        return slots.isEmpty() ? Math.max(1, talentColumns() * talentRows()) : slots.size();
    }

    private int pageForTalentIndex(int talentIndex) {
        return Math.max(0, placement.pageFor(talentIndex));
    }

    private void rebuildPlacement() {
        placement = TalentBookPagination.place(
                visibleTalents().stream().map(PlayerTalentState.TalentView::id).toList(),
                layout.pages(), capacityForPage(layout.pages().size()));
    }

    private static void renderIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size) {
        float scale = size / 16.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(icon, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.pose().popPose();
    }

    private record SlotBounds(int x, int y, int right, int bottom, int iconSize) {
        int height() { return Math.max(1, bottom - y); }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right && mouseY >= y && mouseY < bottom;
        }
    }

    enum BookMode {
        OWNED,
        SYNTHESIS,
        COUNTERS
    }

    private String fit(String value, int maximumWidth) {
        return font.plainSubstrByWidth(value, Math.max(20, maximumWidth));
    }

    private Component fit(PlayerTalentState.StyledTextView value, int maximumWidth, int fallbackColor) {
        Component resolved = value.component(fallbackColor);
        return Component.literal(font.plainSubstrByWidth(resolved.getString(), Math.max(20, maximumWidth)))
                .setStyle(resolved.getStyle());
    }

    private static String signed(double value) {
        String compact = value == Math.rint(value)
                ? Long.toString((long) value)
                : String.format(Locale.ROOT, "%.6f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
        return value >= 0 ? "+" + compact : compact;
    }
}
