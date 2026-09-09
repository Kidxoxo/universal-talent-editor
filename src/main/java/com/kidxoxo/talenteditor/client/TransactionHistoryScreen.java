package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.editor.EditorTransactionSummary;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class TransactionHistoryScreen extends Screen {
    private static final int ROW_HEIGHT = 30;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final EditorBootstrap bootstrap;
    private final List<EditorTransactionSummary> transactions;
    private int scroll;
    private int selected = -1;
    private Button undoButton;

    TransactionHistoryScreen(EditorBootstrap bootstrap, List<EditorTransactionSummary> transactions) {
        super(Component.translatable("screen.talent_editor.history.title"));
        this.bootstrap = bootstrap;
        this.transactions = List.copyOf(transactions);
    }

    @Override
    protected void init() {
        undoButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.talent_editor.history.undo"), button -> openUndo()
        ).bounds(16, height - 28, 92, 20).build());
        undoButton.active = selectedTransaction() != null && selectedTransaction().canUndo();
        addRenderableWidget(Button.builder(
                Component.translatable("screen.talent_editor.history.refresh"), button -> refresh()
        ).bounds(width / 2 - 46, height - 28, 92, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnHome())
                .bounds(width - 86, height - 28, 70, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(
                "screen.talent_editor.history.count", transactions.size()
        ), width / 2, 26, 0xA0A0A0);
        int top = 48;
        int rows = visibleRows(top);
        scroll = clamp(scroll, transactions.size(), rows);
        int end = Math.min(transactions.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            EditorTransactionSummary transaction = transactions.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            if (index == selected) graphics.fill(12, y - 3, width - 12, y + 24, 0x604A90E2);
            else if (mouseX >= 12 && mouseX < width - 12 && mouseY >= y - 3 && mouseY < y + 24) {
                graphics.fill(12, y - 3, width - 12, y + 24, 0x304A90E2);
            }
            Component operation = Component.translatable(
                    "screen.talent_editor.history.operation."
                            + transaction.operation().name().toLowerCase(java.util.Locale.ROOT)
            );
            Component kind = Component.translatable(
                    "screen.talent_editor.references.kind." + transaction.kind().serializedName()
            );
            String first = TIME.format(Instant.ofEpochMilli(transaction.timestamp())) + "  "
                    + operation.getString() + "  " + kind.getString() + "  " + transaction.definitionId();
            graphics.drawString(font, font.plainSubstrByWidth(first, width - 36), 16, y,
                    transaction.canUndo() ? 0xE8E8E8 : 0xA0A0A0, false);
            String state = transaction.canUndo()
                    ? Component.translatable("screen.talent_editor.history.state.available").getString()
                    : Component.translatable(
                            "screen.talent_editor.history.state."
                                    + transaction.state().name().toLowerCase(java.util.Locale.ROOT)
                    ).getString();
            graphics.drawString(font, font.plainSubstrByWidth(
                            state + "  " + transaction.transactionId(), width - 44
                    ), 24, y + 12,
                    transaction.canUndo() ? 0x80D080 : 0x808080, false);
        }
        if (transactions.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.history.empty"),
                    width / 2, top + 12, 0x808080);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= 12 && mouseX < width - 12 && mouseY >= 45 && mouseY < height - 36) {
            int index = scroll + (int) ((mouseY - 48) / ROW_HEIGHT);
            if (index >= scroll && index < transactions.size()) {
                selected = index;
                undoButton.active = transactions.get(index).canUndo();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, transactions.size(), visibleRows(48));
        return true;
    }

    @Override
    public void onClose() {
        returnHome();
    }

    private void openUndo() {
        EditorTransactionSummary transaction = selectedTransaction();
        if (minecraft != null && transaction != null && transaction.canUndo()) {
            minecraft.setScreen(new UndoTransactionScreen(this, bootstrap, transaction));
        }
    }

    private void refresh() {
        EditorNetwork.requestTransactionHistory();
    }

    private void returnHome() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new EditorHomeScreen(latest == null ? bootstrap : latest));
    }

    private EditorTransactionSummary selectedTransaction() {
        return selected >= 0 && selected < transactions.size() ? transactions.get(selected) : null;
    }

    private int visibleRows(int top) {
        return Math.max(1, (height - top - 42) / ROW_HEIGHT);
    }

    private static int clamp(int value, int size, int rows) {
        return Math.max(0, Math.min(value, Math.max(0, size - rows)));
    }
}
