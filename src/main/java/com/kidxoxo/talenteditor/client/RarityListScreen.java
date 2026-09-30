package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class RarityListScreen extends Screen {
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

    public RarityListScreen(EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.rarity_list.title"));
        this.bootstrap = bootstrap;
        this.sources = DefinitionListFilter.raritySources(bootstrap);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.rarity.new"),
                        button -> openNew())
                .bounds(16, 38, 104, 20).build());
        int sourceWidth = Math.min(92, Math.max(64, width / 6));
        int stateWidth = 66;
        int sourceX = width - 16 - sourceWidth;
        int stateX = sourceX - 4 - stateWidth;
        search = new EditBox(font, 126, 38, Math.max(40, stateX - 130), 20,
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
        List<EditorBootstrap.RaritySummary> rarities = filteredRarities();
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.filter.count", rarities.size(), bootstrap.rarities().size()
        ), 16, 66, 0xA0A0A0, false);
        int top = 83;
        int rows = Math.max(1, (height - top - 39) / ROW_HEIGHT);
        scroll = clamp(scroll, rarities.size(), rows);
        int end = Math.min(rarities.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            EditorBootstrap.RaritySummary rarity = rarities.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= 12 && mouseX < width - 12 && mouseY >= y - 3 && mouseY < y + 15;
            if (hovered) graphics.fill(12, y - 3, width - 12, y + 15, 0x504A90E2);
            int color = 0xFF000000 | (rarity.color() & 0xFFFFFF);
            graphics.fill(16, y - 1, 27, y + 10, color);
            String flags = rarity.enabled() ? (rarity.offerable() ? "E/O" : "E/-") : "-/-";
            String label = rarity.id() + " — " + rarity.name() + "  [" + rarity.order()
                    + " / " + compact(rarity.baseWeight()) + " / " + flags + "]  {"
                    + DefinitionListFilter.compactSource(rarity.source()) + "}";
            graphics.drawString(font, font.plainSubstrByWidth(label, width - 53), 33, y,
                    rarity.enabled() ? color : 0xFF808080, false);
        }
        if (rarities.isEmpty()) {
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
            List<EditorBootstrap.RaritySummary> rarities = filteredRarities();
            if (index >= 0 && index < rarities.size()) {
                status = Component.translatable("screen.talent_editor.rarity.loading").getString();
                EditorNetwork.requestRarity(rarities.get(index).id());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        int rows = Math.max(1, (height - 83 - 39) / ROW_HEIGHT);
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, filteredRarities().size(), rows);
        return true;
    }

    private void openNew() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(RarityEditScreen.createNew(bootstrap));
    }

    private List<EditorBootstrap.RaritySummary> filteredRarities() {
        return DefinitionListFilter.rarities(
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
                : Component.translatable(
                        "screen.talent_editor.filter.source",
                        DefinitionListFilter.compactSource(selectedSource)
                );
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

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(java.util.Locale.ROOT, "%.5f", value)
                .replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }
}
