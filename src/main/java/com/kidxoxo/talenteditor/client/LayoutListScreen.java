package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class LayoutListScreen extends EditorScreen {
    private static final int ROW_HEIGHT = EditorTheme.DEFINITION_ROW_HEIGHT;
    private final EditorBootstrap bootstrap;
    private final List<String> sources;
    private EditBox search;
    private Button stateButton;
    private Button sourceButton;
    private DefinitionListFilter.State filterState = DefinitionListFilter.State.ALL;
    private String selectedSource = "";
    private int scroll;
    private int keyboardIndex=-1;
    private String status = "";
    private final EditorOptionPopup options = new EditorOptionPopup();

    public LayoutListScreen(EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.layout_list.title"));
        this.bootstrap = bootstrap;
        this.sources = DefinitionListFilter.layoutSources(bootstrap);
    }

    @Override
    protected void init() {
        var remembered=EditorListMemory.get("Layout");
        filterState=remembered.state();selectedSource=remembered.source();scroll=remembered.scroll();keyboardIndex=remembered.selected();
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.layout.new"), button -> openNew())
                .bounds(16, 38, 104, 20).build());
        int sourceWidth = Math.min(92, Math.max(64, width / 6));
        int stateWidth = 66;
        int sourceX = width - 16 - sourceWidth;
        int stateX = sourceX - 4 - stateWidth;
        search = new EditBox(font, 126, 38, Math.max(40, stateX - 130), 20,
                Component.translatable("screen.talent_editor.filter.search"));
        search.setHint(Component.translatable("screen.talent_editor.filter.search"));
        search.setValue(remembered.query());
        search.setResponder(ignored -> {scroll=0;keyboardIndex=-1;});
        addRenderableWidget(search);
        stateButton = addRenderableWidget(EditorWidgets.button(stateLabel(), button -> chooseState())
                .bounds(stateX, 38, stateWidth, 20).build());
        sourceButton = addRenderableWidget(EditorWidgets.button(sourceLabel(), button -> chooseSource())
                .bounds(sourceX, 38, sourceWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnHome())
                .bounds(width - 86, height - 28, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        List<EditorBootstrap.LayoutSummary> layouts = filteredLayouts();
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.filter.count", layouts.size(), bootstrap.layouts().size()
        ), 16, 66, EditorTheme.MUTED, false);
        int top = 83;
        int rows = Math.max(1, (height - top - 39) / ROW_HEIGHT);
        scroll = clamp(scroll, layouts.size(), rows);
        int end = Math.min(layouts.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            EditorBootstrap.LayoutSummary layout = layouts.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= 12 && mouseX < width - 12 && mouseY >= y && mouseY < y + ROW_HEIGHT;
            int color = layout.active() ? 0x55FFAA : layout.enabled() ? 0xE8E8E8 : 0x808080;
            String marker = layout.active() ? "★ " : "";
            String geometry = layout.freeSlotCount() > 0
                    ? Component.translatable("screen.talent_editor.layout.free_slots", layout.freeSlotCount()).getString()
                    : layout.columns() + "×" + layout.rows();
            if (layout.pageCount() > 0) {
                geometry = Component.translatable(
                        "screen.talent_editor.layout.page_summary", layout.pageCount(), geometry
                ).getString();
            }
            String label = "[" + geometry
                    + " · " + layout.slotWidth() + "×" + layout.slotHeight() + " · P" + layout.priority()
                    + "]  {" + DefinitionListFilter.compactSource(layout.source()) + "}";
            EditorTheme.definitionRow(graphics, font, 12, y, width - 24, layout.active(), hovered, marker + layout.id(), label, color);
        }
        if (layouts.isEmpty()) graphics.drawCenteredString(font,
                Component.translatable("screen.talent_editor.filter.empty"), width / 2, top + 10, 0x808080);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, width - 112),
                16, height - 24, EditorTheme.MUTED, false);
        options.render(graphics, font, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        if (button == 0 && mouseX >= 12 && mouseX < width - 12 && mouseY >= 83 && mouseY < height - 39) {
            int index = scroll + (int) ((mouseY - 83) / ROW_HEIGHT);
            List<EditorBootstrap.LayoutSummary> layouts = filteredLayouts();
            if (index >= 0 && index < layouts.size() && index < scroll + Math.max(1, (height - 83 - 39) / ROW_HEIGHT)) {
                status = Component.translatable("screen.talent_editor.layout.loading").getString();
                EditorNetwork.requestLayout(layouts.get(index).id());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        int rows = Math.max(1, (height - 83 - 39) / ROW_HEIGHT);
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, filteredLayouts().size(), rows);
        return true;
    }

    private List<EditorBootstrap.LayoutSummary> filteredLayouts() {
        return DefinitionListFilter.layouts(
                bootstrap, search == null ? "" : search.getValue(), filterState, selectedSource
        );
    }

    private void chooseState() {
        DefinitionListFilter.chooseState(options, filterState, false, height, value -> {
            filterState = value;
            scroll = 0;
            stateButton.setMessage(stateLabel());
        });
    }

    private void chooseSource() {
        DefinitionListFilter.chooseSource(options, selectedSource, sources, height, value -> {
            selectedSource = value;
            scroll = 0;
            sourceButton.setMessage(sourceLabel());
        });
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        var entries=filteredLayouts();
        if(!entries.isEmpty()&&(keyCode==264||keyCode==265)){
            keyboardIndex=Math.max(0,Math.min(entries.size()-1,keyboardIndex+(keyCode==264?1:-1)));
            int rows=Math.max(1,(height-83-39)/ROW_HEIGHT);scroll=Math.max(0,Math.min(scroll,keyboardIndex));scroll=Math.max(scroll,keyboardIndex-rows+1);return true;
        }
        if((keyCode==257||keyCode==335)&&keyboardIndex>=0&&keyboardIndex<entries.size()){EditorNetwork.requestLayout(entries.get(keyboardIndex).id());return true;}
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }

    private Component stateLabel() {
        return Component.translatable("screen.talent_editor.filter.state." + filterState.translationSuffix());
    }

    private Component sourceLabel() {
        return selectedSource.isBlank()
                ? Component.translatable("screen.talent_editor.filter.source.all")
                : Component.translatable("screen.talent_editor.filter.source",
                        DefinitionListFilter.compactSource(selectedSource));
    }

    private void openNew() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(LayoutEditScreen.createNew(bootstrap));
    }

    private void returnHome() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new EditorHomeScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() { returnHome(); }

    private static int clamp(int value, int size, int rows) {
        return Math.max(0, Math.min(value, Math.max(0, size - rows)));
    }

    @Override public void removed() {
        EditorListMemory.put("Layout",new EditorListMemory.View(search==null?"":search.getValue(),filterState,selectedSource,scroll,keyboardIndex));
        super.removed();
    }
}
