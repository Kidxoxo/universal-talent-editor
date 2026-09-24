package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class EditorHomeScreen extends Screen {
    private static final int ROW_HEIGHT = 18;

    private final EditorBootstrap bootstrap;
    private final List<String> talentSources;
    private EditBox talentSearch;
    private Button talentStateButton;
    private Button talentSourceButton;
    private DefinitionListFilter.State talentState = DefinitionListFilter.State.ALL;
    private String talentSource = "";
    private int talentScroll;
    private String localStatus = "";

    public EditorHomeScreen(EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.home.title"));
        this.bootstrap = bootstrap;
        this.talentSources = DefinitionListFilter.talentSources(bootstrap);
    }

    @Override
    protected void init() {
        int contentWidth = Math.min(900, width - 32);
        int x = (width - contentWidth) / 2;
        int actionGap = 4;
        int actionWidth = Math.max(55, (contentWidth - actionGap * 2) / 3);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.new_talent"),
                        button -> openNew())
                .bounds(x, 45, actionWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.tree.open"),
                        button -> openTalentTree())
                .bounds(x + actionWidth + actionGap, 45, actionWidth, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.manage_rarities"),
                        button -> openRarities())
                .bounds(x + (actionWidth + actionGap) * 2, 45,
                        contentWidth - (actionWidth + actionGap) * 2, 20)
                .build());
        int managementWidth = Math.max(36, (contentWidth - actionGap * 3) / 4);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.manage_pools"),
                        button -> openPools())
                .bounds(x, 69, managementWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.manage_counters"),
                        button -> openCounters())
                .bounds(x + managementWidth + actionGap, 69, managementWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.manage_layouts"),
                        button -> openLayouts())
                .bounds(x + (managementWidth + actionGap) * 2, 69,
                        managementWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.manage_configured_items"),
                        button -> openConfiguredItems())
                .bounds(x + (managementWidth + actionGap) * 3, 69,
                        contentWidth - (managementWidth + actionGap) * 3, 20).build());
        int sourceWidth = Math.min(90, Math.max(58, contentWidth / 5));
        int stateWidth = 44;
        int sourceX = x + contentWidth - sourceWidth;
        int stateX = sourceX - 4 - stateWidth;
        talentSearch = new EditBox(font, x, 94,
                Math.max(28, stateX - x - 4), 20,
                Component.translatable("screen.talent_editor.filter.search"));
        talentSearch.setHint(Component.translatable("screen.talent_editor.filter.search"));
        talentSearch.setResponder(ignored -> talentScroll = 0);
        addRenderableWidget(talentSearch);
        talentStateButton = addRenderableWidget(Button.builder(talentStateLabel(), button -> cycleTalentState())
                .bounds(stateX, 94, stateWidth, 20).build());
        talentSourceButton = addRenderableWidget(Button.builder(talentSourceLabel(), button -> cycleTalentSource())
                .bounds(sourceX, 94, sourceWidth, 20).build());
        int bottomGap = 4;
        int bottomWidth = Math.max(40, (width - 32 - bottomGap * 3) / 4);
        int bottomX = 16;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.manage_settings"),
                        button -> openSettings())
                .bounds(bottomX, height - 28, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.manage_resources"),
                        button -> openResources())
                .bounds(bottomX + bottomWidth + bottomGap, height - 28, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.history.open"),
                        button -> openHistory())
                .bounds(bottomX + (bottomWidth + bottomGap) * 2, height - 28, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(bottomX + (bottomWidth + bottomGap) * 3, height - 28,
                        width - bottomX * 2 - (bottomWidth + bottomGap) * 3, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(
                "screen.talent_editor.revision", bootstrap.revision(), shortHash(bootstrap.contentHash())
        ), width / 2, 25, 0xA0A0A0);

        int contentWidth = Math.min(900, width - 32);
        int x = (width - contentWidth) / 2;
        List<EditorBootstrap.TalentSummary> talents = filteredTalents();
        graphics.drawString(font, Component.translatable("screen.talent_editor.talents",
                Component.translatable("screen.talent_editor.filter.count", talents.size(), bootstrap.talents().size())),
                x, 121, 0xFFFFFF, false);
        renderTalents(graphics, mouseX, mouseY, x, contentWidth);
        if (!localStatus.isBlank()) {
            graphics.drawString(font, fit(localStatus, width - 32), 16, height - 40, 0xA0A0A0, false);
        }
    }

    private void renderTalents(GuiGraphics graphics, int mouseX, int mouseY, int x, int contentWidth) {
        List<EditorBootstrap.TalentSummary> talents = filteredTalents();
        int top = 137;
        int visibleRows = visibleRows(top);
        talentScroll = clampScroll(talentScroll, talents.size(), visibleRows);
        int end = Math.min(talents.size(), talentScroll + visibleRows);
        for (int index = talentScroll; index < end; index++) {
            EditorBootstrap.TalentSummary talent = talents.get(index);
            int y = top + (index - talentScroll) * ROW_HEIGHT;
            if (hovered(mouseX, mouseY, x, x + contentWidth, y)) {
                graphics.fill(x, y - 3, x + contentWidth, y + 14, 0x504A90E2);
            }
            int color = talent.enabled() && talent.available() ? 0xE8E8E8 : 0x909090;
            String label = talent.id() + " — " + talent.name() + "  {"
                    + DefinitionListFilter.compactSource(talent.source()) + "}";
            graphics.drawString(font, fit(label, contentWidth - 8), x + 4, y, color, false);
        }
        if (talents.isEmpty()) graphics.drawCenteredString(font,
                Component.translatable("screen.talent_editor.filter.empty"),
                width / 2, top + 10, 0x808080);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int contentWidth = Math.min(900, width - 32);
            int x = (width - contentWidth) / 2;
            if (mouseX >= x && mouseX < x + contentWidth && mouseY >= 137 && mouseY < height - 38) {
                int row = (int) ((mouseY - 137) / ROW_HEIGHT);
                List<EditorBootstrap.TalentSummary> talents = filteredTalents();
                int index = talentScroll + row;
                if (index >= 0 && index < talents.size()) {
                    localStatus = Component.translatable("screen.talent_editor.loading_talent").getString();
                    EditorNetwork.requestTalent(talents.get(index).id());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = (int) Math.signum(delta) * 3;
        int visibleRows = visibleRows(137);
        talentScroll = clampScroll(talentScroll - step, filteredTalents().size(), visibleRows);
        return true;
    }

    private void openNew() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(TalentEditScreen.createNew(bootstrap));
    }

    private void openTalentTree() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(new TalentTreeScreen(bootstrap));
    }

    private void openPools() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(new PoolListScreen(bootstrap));
    }

    private void openRarities() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(new RarityListScreen(bootstrap));
    }

    private void openCounters() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(new CounterListScreen(bootstrap));
    }

    private void openLayouts() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(new LayoutListScreen(bootstrap));
    }

    private void openConfiguredItems() {
        if (minecraft == null) return;
        ClientEditorState.clearValidation();
        minecraft.setScreen(new ConfiguredItemListScreen(bootstrap));
    }

    private void openHistory() {
        localStatus = Component.translatable("screen.talent_editor.history.loading").getString();
        EditorNetwork.requestTransactionHistory();
    }

    private void openResources() {
        if (minecraft != null) minecraft.setScreen(new ClientResourceImportScreen(this));
    }

    private void openSettings() {
        if (minecraft != null) minecraft.setScreen(new EditorSettingsScreen(bootstrap));
    }

    private List<EditorBootstrap.TalentSummary> filteredTalents() {
        return DefinitionListFilter.talents(
                bootstrap, talentSearch == null ? "" : talentSearch.getValue(), talentState, talentSource
        );
    }

    private void cycleTalentState() {
        talentState = talentState.next(true);
        talentScroll = 0;
        talentStateButton.setMessage(talentStateLabel());
    }

    private void cycleTalentSource() {
        talentSource = DefinitionListFilter.nextSource(talentSource, talentSources);
        talentScroll = 0;
        talentSourceButton.setMessage(talentSourceLabel());
    }

    private Component talentStateLabel() {
        return Component.translatable("screen.talent_editor.filter.state." + talentState.translationSuffix());
    }

    private Component talentSourceLabel() {
        return talentSource.isBlank()
                ? Component.translatable("screen.talent_editor.filter.source.all")
                : Component.translatable(
                        "screen.talent_editor.filter.source",
                        DefinitionListFilter.compactSource(talentSource)
                );
    }

    private int visibleRows(int top) {
        return Math.max(1, (height - top - 38) / ROW_HEIGHT);
    }

    private static int clampScroll(int scroll, int size, int rows) {
        return Math.max(0, Math.min(scroll, Math.max(0, size - rows)));
    }

    private static boolean hovered(double mouseX, double mouseY, int left, int right, int y) {
        return mouseX >= left && mouseX < right && mouseY >= y - 3 && mouseY < y + 14;
    }

    private String fit(String value, int maximumWidth) {
        return font.plainSubstrByWidth(value, Math.max(20, maximumWidth));
    }

    private static String shortHash(String value) {
        return value.length() <= 12 ? value : value.substring(0, 12);
    }
}
