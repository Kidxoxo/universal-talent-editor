package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ConfiguredItemListScreen extends Screen {
    private static final int ROW_HEIGHT = 20;

    private final EditorBootstrap bootstrap;
    private final List<String> sources;
    private EditBox search;
    private Button stateButton;
    private Button sourceButton;
    private DefinitionListFilter.State filterState = DefinitionListFilter.State.ALL;
    private String selectedSource = "";
    private int scroll;
    private String status = "";
    private final EditorOptionPopup options = new EditorOptionPopup();

    public ConfiguredItemListScreen(EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.configured_item_list.title"));
        this.bootstrap = bootstrap;
        this.sources = DefinitionListFilter.configuredItemSources(bootstrap);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.configured_item.new"),
                        button -> openNew())
                .bounds(16, 38, 112, 20).build());
        int sourceWidth = Math.min(92, Math.max(64, width / 6));
        int stateWidth = 66;
        int sourceX = width - 16 - sourceWidth;
        int stateX = sourceX - 4 - stateWidth;
        search = new EditBox(font, 134, 38, Math.max(40, stateX - 138), 20,
                Component.translatable("screen.talent_editor.filter.search"));
        search.setHint(Component.translatable("screen.talent_editor.filter.search"));
        search.setResponder(ignored -> scroll = 0);
        addRenderableWidget(search);
        stateButton = addRenderableWidget(Button.builder(stateLabel(), button -> chooseState())
                .bounds(stateX, 38, stateWidth, 20).build());
        sourceButton = addRenderableWidget(Button.builder(sourceLabel(), button -> chooseSource())
                .bounds(sourceX, 38, sourceWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnHome())
                .bounds(width - 86, height - 28, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        List<EditorBootstrap.ConfiguredItemSummary> items = filteredItems();
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.filter.count", items.size(), bootstrap.configuredItems().size()
        ), 16, 66, 0xA0A0A0, false);
        int top = 83;
        int rows = Math.max(1, (height - top - 39) / ROW_HEIGHT);
        scroll = clamp(scroll, items.size(), rows);
        int end = Math.min(items.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            EditorBootstrap.ConfiguredItemSummary item = items.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= 12 && mouseX < width - 12 && mouseY >= y - 3 && mouseY < y + 15;
            if (hovered) graphics.fill(12, y - 3, width - 12, y + 15, 0x504A90E2);
            int color = item.enabled() && item.available() ? 0xFFE8E8E8 : 0xFF808080;
            String label = item.id() + " — " + item.name() + "  [model=" + item.customModelData()
                    + (item.glint() ? ", glint" : "") + "]  {"
                    + DefinitionListFilter.compactSource(item.source()) + "}";
            graphics.drawString(font, font.plainSubstrByWidth(label, width - 32), 16, y, color, false);
        }
        if (items.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.filter.empty"),
                    width / 2, top + 10, 0x808080);
        }
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, width - 112),
                    16, height - 24, 0xA0A0A0, false);
        }
        options.render(graphics, font, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        if (button == 0 && mouseX >= 12 && mouseX < width - 12 && mouseY >= 83 && mouseY < height - 39) {
            int index = scroll + (int) ((mouseY - 83) / ROW_HEIGHT);
            List<EditorBootstrap.ConfiguredItemSummary> items = filteredItems();
            if (index >= 0 && index < items.size()) {
                status = Component.translatable("screen.talent_editor.configured_item.loading").getString();
                EditorNetwork.requestConfiguredItem(items.get(index).id());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        int rows = Math.max(1, (height - 83 - 39) / ROW_HEIGHT);
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, filteredItems().size(), rows);
        return true;
    }

    private List<EditorBootstrap.ConfiguredItemSummary> filteredItems() {
        return DefinitionListFilter.configuredItems(
                bootstrap, search == null ? "" : search.getValue(), filterState, selectedSource
        );
    }

    private void openNew() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(ConfiguredItemEditScreen.createNew(bootstrap));
    }

    private void chooseState() {
        DefinitionListFilter.chooseState(options, filterState, true, height, value -> {
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

    private void returnHome() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new EditorHomeScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnHome();
    }

    private static int clamp(int value, int size, int rows) {
        return Math.max(0, Math.min(value, Math.max(0, size - rows)));
    }
}
