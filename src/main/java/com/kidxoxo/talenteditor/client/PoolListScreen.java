package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class PoolListScreen extends Screen {
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

    public PoolListScreen(EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.pool_list.title"));
        this.bootstrap = bootstrap;
        this.sources = DefinitionListFilter.poolSources(bootstrap);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.pool.new"),
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
        stateButton = addRenderableWidget(Button.builder(stateLabel(), button -> cycleState())
                .bounds(stateX, 38, stateWidth, 20).build());
        sourceButton = addRenderableWidget(Button.builder(sourceLabel(), button -> cycleSource())
                .bounds(sourceX, 38, sourceWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnHome())
                .bounds(width - 86, height - 28, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        List<EditorBootstrap.PoolSummary> pools = filteredPools();
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.filter.count", pools.size(), bootstrap.pools().size()
        ), 16, 66, 0xA0A0A0, false);
        int top = 83;
        int rows = Math.max(1, (height - top - 39) / ROW_HEIGHT);
        scroll = clamp(scroll, pools.size(), rows);
        int end = Math.min(pools.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            EditorBootstrap.PoolSummary pool = pools.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= 12 && mouseX < width - 12 && mouseY >= y - 3 && mouseY < y + 15;
            if (hovered) graphics.fill(12, y - 3, width - 12, y + 15, 0x504A90E2);
            String label = pool.id() + " — " + pool.name() + "  [" + pool.choices() + "/"
                    + pool.refreshes() + "/" + pool.maxOwned() + "]  {"
                    + DefinitionListFilter.compactSource(pool.source()) + "}";
            graphics.drawString(font, font.plainSubstrByWidth(label, width - 32), 16, y,
                    pool.enabled() ? 0xE8E8E8 : 0x909090, false);
        }
        if (pools.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(
                            bootstrap.pools().isEmpty()
                                    ? "screen.talent_editor.pool_list.empty"
                                    : "screen.talent_editor.filter.empty"),
                    width / 2, top + 10, 0x808080);
        }
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, width - 112),
                    16, height - 24, 0xA0A0A0, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= 12 && mouseX < width - 12 && mouseY >= 83 && mouseY < height - 39) {
            int index = scroll + (int) ((mouseY - 83) / ROW_HEIGHT);
            List<EditorBootstrap.PoolSummary> pools = filteredPools();
            if (index >= 0 && index < pools.size()) {
                status = Component.translatable("screen.talent_editor.pool.loading").getString();
                EditorNetwork.requestPool(pools.get(index).id());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = Math.max(1, (height - 83 - 39) / ROW_HEIGHT);
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, filteredPools().size(), rows);
        return true;
    }

    private void openNew() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(PoolEditScreen.createNew(bootstrap));
    }

    private List<EditorBootstrap.PoolSummary> filteredPools() {
        return DefinitionListFilter.pools(
                bootstrap, search == null ? "" : search.getValue(), filterState, selectedSource
        );
    }

    private void cycleState() {
        filterState = filterState.next(false);
        scroll = 0;
        stateButton.setMessage(stateLabel());
    }

    private void cycleSource() {
        selectedSource = DefinitionListFilter.nextSource(selectedSource, sources);
        scroll = 0;
        sourceButton.setMessage(sourceLabel());
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
}
