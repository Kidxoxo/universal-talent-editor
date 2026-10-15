package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.editor.DefinitionUndoStatus;
import com.kidxoxo.talenteditor.editor.EditorTransactionSummary;
import com.kidxoxo.talenteditor.network.DefinitionUndoResultPacket;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class UndoTransactionScreen extends EditorScreen {
    private final Screen parent;
    private final EditorTransactionSummary transaction;
    private long baseRevision;
    private Button confirmButton;
    private Component status;
    private boolean waiting;

    UndoTransactionScreen(
            Screen parent,
            EditorBootstrap bootstrap,
            EditorTransactionSummary transaction
    ) {
        super(Component.translatable("screen.talent_editor.undo.title"));
        this.parent = parent;
        this.baseRevision = bootstrap.revision();
        this.transaction = transaction;
    }

    @Override
    protected void init() {
        int center = width / 2;
        confirmButton = addRenderableWidget(EditorWidgets.button(
                Component.translatable("screen.talent_editor.undo.confirm"), button -> submit()
        ).bounds(center - 104, height - 32, 100, 20).build());
        confirmButton.active = !waiting && transaction.canUndo();
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnParent())
                .bounds(center + 4, height - 32, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawWordWrap(font, Component.translatable(
                "screen.talent_editor.undo.target",
                Component.translatable("screen.talent_editor.history.operation."
                        + transaction.operation().name().toLowerCase(Locale.ROOT)),
                Component.translatable("screen.talent_editor.references.kind."
                        + transaction.kind().serializedName()),
                transaction.definitionId().toString()
        ), 24, 48, width - 48, 0xE8E8E8);
        graphics.drawWordWrap(font, Component.translatable(
                "screen.talent_editor.undo.effect."
                        + transaction.operation().name().toLowerCase(Locale.ROOT)
        ), 24, 78, width - 48, 0x80D8FF);
        graphics.drawWordWrap(font, Component.translatable(
                "screen.talent_editor.undo.warning"
        ), 24, 104, width - 48, 0xFFAA55);
        if (status != null) {
            graphics.drawWordWrap(font, status, 24, height - 64, width - 48, 0xFF8080);
        }
    }

    void acceptResult(DefinitionUndoResultPacket packet) {
        if (!packet.transactionId().equals(transaction.transactionId())) return;
        waiting = false;
        baseRevision = packet.activeRevision();
        status = Component.translatable(
                "screen.talent_editor.undo.result." + packet.status().name().toLowerCase(Locale.ROOT)
        );
        if (confirmButton != null) {
            confirmButton.active = packet.status() == DefinitionUndoStatus.IO_FAILED
                    || packet.status() == DefinitionUndoStatus.RELOAD_FAILED;
        }
    }

    @Override
    public void onClose() {
        if (!waiting) returnParent();
    }

    private void submit() {
        waiting = true;
        status = Component.translatable("screen.talent_editor.undo.waiting");
        confirmButton.active = false;
        EditorNetwork.undoTransaction(baseRevision, transaction.transactionId());
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
