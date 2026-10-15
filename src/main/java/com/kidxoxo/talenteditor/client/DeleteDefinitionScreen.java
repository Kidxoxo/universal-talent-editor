package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.editor.DefinitionDeletionStatus;
import com.kidxoxo.talenteditor.network.DefinitionDeletionResultPacket;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

final class DeleteDefinitionScreen extends EditorScreen {
    private final Screen parent;
    private final DefinitionKind kind;
    private final ResourceLocation id;
    private final String source;
    private long baseRevision;
    private Button confirmButton;
    private Component status;
    private boolean waiting;

    DeleteDefinitionScreen(
            Screen parent,
            EditorBootstrap bootstrap,
            DefinitionKind kind,
            ResourceLocation id,
            String source
    ) {
        super(Component.translatable("screen.talent_editor.delete.title"));
        this.parent = parent;
        this.baseRevision = bootstrap.revision();
        this.kind = kind;
        this.id = id;
        this.source = source;
    }

    @Override
    protected void init() {
        int center = width / 2;
        confirmButton = addRenderableWidget(EditorWidgets.button(
                Component.translatable("screen.talent_editor.delete.confirm"), button -> submit()
        ).bounds(center - 104, height - 32, 100, 20).build());
        confirmButton.active = !waiting;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnParent())
                .bounds(center + 4, height - 32, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawWordWrap(font, Component.translatable(
                "screen.talent_editor.delete.target",
                Component.translatable("screen.talent_editor.references.kind." + kind.serializedName()),
                id.toString()
        ), 24, 48, width - 48, 0xE8E8E8);
        graphics.drawWordWrap(font, Component.translatable(
                "screen.talent_editor.delete.source", source
        ), 24, 76, width - 48, 0xA0A0A0);
        graphics.drawWordWrap(font, Component.translatable(
                "screen.talent_editor.delete.warning"
        ), 24, 106, width - 48, 0xFFAA55);
        if (status != null) {
            graphics.drawWordWrap(font, status, 24, height - 62, width - 48, 0xFF8080);
        }
    }

    void acceptResult(DefinitionDeletionResultPacket packet) {
        if (packet.kind() != kind || !packet.id().equals(id)) return;
        waiting = false;
        baseRevision = packet.activeRevision();
        String suffix = packet.status().name().toLowerCase(Locale.ROOT);
        status = packet.status() == DefinitionDeletionStatus.BLOCKED_REFERENCES
                ? Component.translatable(
                        "screen.talent_editor.delete.result." + suffix,
                        packet.blockingReferences().size()
                )
                : Component.translatable("screen.talent_editor.delete.result." + suffix);
        if (confirmButton != null) {
            confirmButton.active = packet.status() == DefinitionDeletionStatus.IO_FAILED
                    || packet.status() == DefinitionDeletionStatus.VALIDATION_FAILED
                    || packet.status() == DefinitionDeletionStatus.RELOAD_FAILED;
        }
    }

    @Override
    public void onClose() {
        if (!waiting) returnParent();
    }

    private void submit() {
        waiting = true;
        status = Component.translatable("screen.talent_editor.delete.waiting");
        confirmButton.active = false;
        EditorNetwork.deleteDefinition(baseRevision, kind, id);
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
