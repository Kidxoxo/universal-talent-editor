package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.ItemSubmissionOpenPacket;
import com.kidxoxo.talenteditor.network.ItemSubmissionResultPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemSubmissionScreen extends Screen {
    private final ItemSubmissionOpenPacket session;
    private Button confirm;
    private String status = "";
    private int ticksRemaining;
    private boolean completed;

    public ItemSubmissionScreen(ItemSubmissionOpenPacket session) {
        super(Component.translatable("screen.talent_editor.item_submission.title"));
        this.session = session;
        this.ticksRemaining = session.timeoutTicks();
    }

    @Override protected void init() {
        int panel = Math.min(360, width - 32);
        int x = (width - panel) / 2;
        confirm = addRenderableWidget(Button.builder(
                Component.translatable("screen.talent_editor.item_submission.confirm"), button -> confirm())
                .bounds(x, height / 2 + 34, (panel - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> cancel())
                .bounds(x + (panel + 6) / 2, height / 2 + 34, (panel - 6) / 2, 20).build());
        updateButton();
    }

    @Override public void tick() {
        super.tick();
        if (ticksRemaining > 0) ticksRemaining--;
        updateButton();
        if (ticksRemaining == 0 && status.isBlank()) {
            status = Component.translatable("screen.talent_editor.item_submission.result.expired").getString();
        }
    }

    private void updateButton() {
        if (confirm != null) confirm.active = ticksRemaining > 0 && ownedCount() >= session.count();
    }

    private int ownedCount() {
        if (minecraft == null || minecraft.player == null) return 0;
        Item wanted = ForgeRegistries.ITEMS.getValue(session.item());
        if (wanted == null) return 0;
        int count = 0;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.is(wanted)) count += stack.getCount();
        }
        return count;
    }

    private void confirm() {
        status = Component.translatable("screen.talent_editor.item_submission.waiting").getString();
        confirm.active = false;
        EditorNetwork.submitItemSubmission(session.sessionId(), true);
    }

    private void cancel() {
        if (completed) return;
        completed = true;
        EditorNetwork.submitItemSubmission(session.sessionId(), false);
        if (minecraft != null) minecraft.setScreen(null);
    }

    public void accept(ItemSubmissionResultPacket result) {
        if (!result.sessionId().equals(session.sessionId())) return;
        status = Component.translatable("screen.talent_editor.item_submission.result."
                + result.status().name().toLowerCase(java.util.Locale.ROOT)).getString();
        if (result.status() == ItemSubmissionResultPacket.Status.ACCEPTED && minecraft != null) {
            completed = true;
            minecraft.setScreen(null);
        } else updateButton();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panel = Math.min(360, width - 32);
        int x = (width - panel) / 2;
        int y = height / 2 - 58;
        EditorTheme.panel(graphics, x - 8, y - 16, panel + 16, 128);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, y - 8, EditorTheme.TEXT);
        Item item = ForgeRegistries.ITEMS.getValue(session.item());
        ItemStack stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
        if (!stack.isEmpty()) graphics.renderItem(stack, x + 8, y + 18);
        Component detail = Component.translatable("screen.talent_editor.item_submission.detail",
                session.count(), stack.isEmpty() ? session.item().toString() : stack.getHoverName(), ownedCount());
        graphics.drawString(font, font.plainSubstrByWidth(detail.getString(), panel - 42), x + 32, y + 22,
                EditorTheme.TEXT, false);
        graphics.drawString(font, Component.translatable(session.consume()
                        ? "screen.talent_editor.item_submission.consume" : "screen.talent_editor.item_submission.check"),
                x + 8, y + 47, EditorTheme.MUTED, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.item_submission.timeout",
                (ticksRemaining + 19) / 20), x + 8, y + 62, EditorTheme.MUTED, false);
        if (!status.isBlank()) graphics.drawCenteredString(font, status, width / 2, y + 82, 0xFFFFAA55);
    }

    @Override public void onClose() {
        if (!completed) {
            completed = true;
            EditorNetwork.submitItemSubmission(session.sessionId(), false);
        }
        super.onClose();
    }
}
