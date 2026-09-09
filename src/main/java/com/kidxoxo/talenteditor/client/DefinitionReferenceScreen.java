package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

final class DefinitionReferenceScreen extends Screen {
    private static final int ROW_HEIGHT = 28;

    private final Screen parent;
    private final EditorBootstrap bootstrap;
    private final DefinitionKind targetKind;
    private final ResourceLocation targetId;
    private final int totalCount;
    private final List<DefinitionReference> references;
    private int scroll;
    private Button deleteButton;

    DefinitionReferenceScreen(
            Screen parent,
            EditorBootstrap bootstrap,
            DefinitionKind targetKind,
            ResourceLocation targetId,
            int totalCount
    ) {
        super(Component.translatable("screen.talent_editor.references.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.targetKind = targetKind;
        this.targetId = targetId;
        this.totalCount = Math.max(totalCount, 0);
        this.references = bootstrap.references().stream()
                .filter(reference -> reference.target().kind() == targetKind
                        && reference.target().id().equals(targetId))
                .toList();
    }

    @Override
    protected void init() {
        String source = source();
        boolean generated = isGeneratedSource(source);
        Component deleteLabel = !generated
                ? Component.translatable("screen.talent_editor.delete.read_only")
                : totalCount > 0
                ? Component.translatable("screen.talent_editor.delete.blocked", totalCount)
                : Component.translatable("screen.talent_editor.delete.open");
        deleteButton = addRenderableWidget(Button.builder(deleteLabel, button -> openDeletion(source))
                .bounds(16, height - 28, 132, 20).build());
        deleteButton.active = generated && totalCount == 0 && fileBacked(targetKind);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnParent())
                .bounds(width - 86, height - 28, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        String target = kindName(targetKind) + " " + targetId;
        graphics.drawString(font, font.plainSubstrByWidth(target, width - 32), 16, 28, 0xE8E8E8, false);
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.references.count", references.size(), totalCount
        ), 16, 42, 0xA0A0A0, false);

        int top = 63;
        int rows = Math.max(1, (height - top - 39) / ROW_HEIGHT);
        scroll = clamp(scroll, references.size(), rows);
        int end = Math.min(references.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            DefinitionReference reference = references.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            if (mouseX >= 12 && mouseX < width - 12 && mouseY >= y - 3 && mouseY < y + 23) {
                graphics.fill(12, y - 3, width - 12, y + 23, 0x404A90E2);
            }
            String source = kindName(reference.sourceKind()) + " " + reference.sourceId();
            graphics.drawString(font, font.plainSubstrByWidth(source, width - 36), 16, y,
                    sourceColor(reference.sourceKind()), false);
            String detail = usageName(reference.usage()) + "  " + reference.path();
            graphics.drawString(font, font.plainSubstrByWidth(detail, width - 44), 24, y + 11,
                    0x909090, false);
        }
        if (references.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.references.empty"),
                    width / 2, top + 8, 0x808080);
        }
        if (references.size() < totalCount) {
            graphics.drawString(font, font.plainSubstrByWidth(
                            Component.translatable("screen.talent_editor.references.truncated").getString(),
                            width - 112),
                    16, height - 24, 0xFFAA55, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = Math.max(1, (height - 63 - 39) / ROW_HEIGHT);
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, references.size(), rows);
        return true;
    }

    @Override
    public void onClose() {
        returnParent();
    }

    static Component buttonLabel(int count) {
        return Component.translatable("screen.talent_editor.references.button", Math.max(count, 0));
    }

    static int inboundCount(EditorBootstrap bootstrap, DefinitionKind kind, ResourceLocation id) {
        return switch (kind) {
            case TALENT -> bootstrap.talents().stream().filter(value -> value.id().equals(id))
                    .mapToInt(EditorBootstrap.TalentSummary::inboundReferences).findFirst().orElse(0);
            case RARITY -> bootstrap.rarities().stream().filter(value -> value.id().equals(id))
                    .mapToInt(EditorBootstrap.RaritySummary::inboundReferences).findFirst().orElse(0);
            case POOL -> bootstrap.pools().stream().filter(value -> value.id().equals(id))
                    .mapToInt(EditorBootstrap.PoolSummary::inboundReferences).findFirst().orElse(0);
            case COUNTER -> bootstrap.counters().stream().filter(value -> value.id().equals(id))
                    .mapToInt(EditorBootstrap.CounterSummary::inboundReferences).findFirst().orElse(0);
            case LAYOUT -> bootstrap.layouts().stream().filter(value -> value.id().equals(id))
                    .mapToInt(EditorBootstrap.LayoutSummary::inboundReferences).findFirst().orElse(0);
            case ITEM -> bootstrap.configuredItems().stream().filter(value -> value.id().equals(id))
                    .mapToInt(EditorBootstrap.ConfiguredItemSummary::inboundReferences).findFirst().orElse(0);
            case TALENT_CATEGORY, COUNTER_CATEGORY -> (int) bootstrap.references().stream()
                    .filter(reference -> reference.target().kind() == kind && reference.target().id().equals(id))
                    .count();
        };
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void openDeletion(String source) {
        if (minecraft != null) {
            minecraft.setScreen(new DeleteDefinitionScreen(
                    this, bootstrap, targetKind, targetId, source
            ));
        }
    }

    private String source() {
        return switch (targetKind) {
            case TALENT -> bootstrap.talents().stream().filter(value -> value.id().equals(targetId))
                    .map(EditorBootstrap.TalentSummary::source).findFirst().orElse("unknown");
            case RARITY -> bootstrap.rarities().stream().filter(value -> value.id().equals(targetId))
                    .map(EditorBootstrap.RaritySummary::source).findFirst().orElse("unknown");
            case POOL -> bootstrap.pools().stream().filter(value -> value.id().equals(targetId))
                    .map(EditorBootstrap.PoolSummary::source).findFirst().orElse("unknown");
            case COUNTER -> bootstrap.counters().stream().filter(value -> value.id().equals(targetId))
                    .map(EditorBootstrap.CounterSummary::source).findFirst().orElse("unknown");
            case LAYOUT -> bootstrap.layouts().stream().filter(value -> value.id().equals(targetId))
                    .map(EditorBootstrap.LayoutSummary::source).findFirst().orElse("unknown");
            case ITEM -> bootstrap.configuredItems().stream().filter(value -> value.id().equals(targetId))
                    .map(EditorBootstrap.ConfiguredItemSummary::source).findFirst().orElse("unknown");
            case TALENT_CATEGORY, COUNTER_CATEGORY -> "virtual";
        };
    }

    private static boolean fileBacked(DefinitionKind kind) {
        return kind == DefinitionKind.TALENT || kind == DefinitionKind.RARITY
                || kind == DefinitionKind.POOL || kind == DefinitionKind.COUNTER
                || kind == DefinitionKind.LAYOUT || kind == DefinitionKind.ITEM;
    }

    private static boolean isGeneratedSource(String source) {
        String normalized = source.replace('\\', '/');
        return normalized.equals("talent_editor_generated")
                || normalized.equals("file/talent_editor_generated")
                || normalized.endsWith("/talent_editor_generated");
    }

    private String kindName(DefinitionKind kind) {
        return Component.translatable("screen.talent_editor.references.kind." + kind.serializedName()).getString();
    }

    private String usageName(String usage) {
        return Component.translatable("screen.talent_editor.references.usage." + usage).getString();
    }

    private static int sourceColor(DefinitionKind kind) {
        return switch (kind) {
            case TALENT -> 0xE8E8E8;
            case RARITY -> 0xE0A0FF;
            case POOL -> 0x80D8FF;
            case COUNTER -> 0xFFD080;
            case LAYOUT -> 0x80FFCC;
            case ITEM -> 0xD8B080;
            case TALENT_CATEGORY, COUNTER_CATEGORY -> 0xA0A0A0;
        };
    }

    private static int clamp(int value, int size, int rows) {
        return Math.max(0, Math.min(value, Math.max(0, size - rows)));
    }
}
