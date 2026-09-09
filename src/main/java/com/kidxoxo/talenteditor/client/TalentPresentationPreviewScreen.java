package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.Locale;

/** Client-only visual and audio preview; it never grants a talent or contacts the server. */
public final class TalentPresentationPreviewScreen extends Screen {
    private final Screen parent;
    private final TalentDraft.PresentationDraft draft;
    private ClientResourceDiagnostics.Result soundResource =
            new ClientResourceDiagnostics.Result(ClientResourceDiagnostics.State.EMPTY, null);
    private SoundInstance previewSound;

    public TalentPresentationPreviewScreen(Screen parent, TalentDraft.PresentationDraft draft) {
        super(Component.translatable("screen.talent_editor.presentation.preview_title"));
        this.parent = parent;
        this.draft = draft;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnParent())
                .bounds(width / 2 - 55, height - 30, 110, 20).build());
        previewSound();
    }

    private void previewSound() {
        if (!draft.soundEnabled() || minecraft == null) return;
        soundResource = ClientResourceDiagnostics.inspect(
                draft.sound().toString(), id -> minecraft.getSoundManager().getSoundEvent(id) != null
        );
        if (!soundResource.present()) return;
        SoundSource source;
        try {
            source = SoundSource.valueOf(draft.soundSource().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            source = SoundSource.PLAYERS;
        }
        previewSound = new SimpleSoundInstance(
                draft.sound(), source, (float) draft.volume(), (float) draft.pitch(), RandomSource.create(),
                false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true
        );
        minecraft.getSoundManager().play(previewSound);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = Math.min(680, width - 32);
        int panelX = (width - panelWidth) / 2;
        int panelTop = 24;
        int panelBottom = height - 42;
        EditorTheme.panel(graphics, panelX, panelTop, panelWidth, Math.max(1, panelBottom - panelTop));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("screen.talent_editor.presentation.preview_local_only"),
                width / 2, panelTop + 10, EditorTheme.MUTED);

        renderSoundState(graphics, panelX + 12, panelTop + 28, panelWidth - 24);
        if (!draft.messageEnabled()) {
            graphics.drawCenteredString(font,
                    Component.translatable(draft.soundEnabled()
                            ? "screen.talent_editor.presentation.preview_sound_only"
                            : "screen.talent_editor.presentation.preview_empty"),
                    width / 2, (panelTop + panelBottom) / 2, EditorTheme.MUTED);
            return;
        }

        Component main = draft.text().component();
        Component subtitle = draft.subtitle().component();
        String mode = draft.mode();
        if (mode.equals("title")) {
            renderCenteredLines(graphics, main, panelX + 22, panelWidth - 44,
                    (panelTop + panelBottom) / 2 - 18, 0xFFFFFFFF);
            if (draft.subtitleEnabled()) {
                renderCenteredLines(graphics, subtitle, panelX + 30, panelWidth - 60,
                        (panelTop + panelBottom) / 2 + 15, 0xFFFFFFFF);
            }
            graphics.drawCenteredString(font, Component.translatable(
                            "screen.talent_editor.presentation.preview_timing",
                            draft.fadeInTicks(), draft.stayTicks(), draft.fadeOutTicks()),
                    width / 2, panelBottom - 18, EditorTheme.MUTED);
        } else if (mode.equals("action_bar")) {
            int boxY = panelBottom - 48;
            graphics.fill(panelX + 20, boxY - 6, panelX + panelWidth - 20, boxY + 22, 0x80000000);
            renderCenteredLines(graphics, main, panelX + 26, panelWidth - 52, boxY, 0xFFFFFFFF);
        } else {
            int boxY = panelBottom - 68;
            graphics.fill(panelX + 12, boxY - 7, panelX + panelWidth - 12, panelBottom - 8, 0x90000000);
            graphics.drawString(font, Component.translatable("screen.talent_editor.presentation.preview_chat"),
                    panelX + 18, boxY - 1, EditorTheme.MUTED, false);
            renderLeftLines(graphics, main, panelX + 18, panelWidth - 36, boxY + 12, 0xFFFFFFFF);
        }
    }

    private void renderSoundState(GuiGraphics graphics, int x, int y, int availableWidth) {
        Component message;
        int color;
        if (!draft.soundEnabled()) {
            message = Component.translatable("screen.talent_editor.presentation.preview_sound_disabled");
            color = EditorTheme.MUTED;
        } else if (soundResource.state() == ClientResourceDiagnostics.State.AVAILABLE) {
            message = Component.translatable("screen.talent_editor.resource.sound_available", draft.sound());
            color = 0xFF77DD88;
        } else {
            message = Component.translatable("screen.talent_editor.resource.sound_missing", draft.sound());
            color = 0xFFFFAA55;
        }
        graphics.drawString(font, font.plainSubstrByWidth(message.getString(), availableWidth), x, y, color, false);
    }

    private void renderCenteredLines(
            GuiGraphics graphics, Component text, int x, int availableWidth, int centerY, int color
    ) {
        List<FormattedCharSequence> lines = font.split(text, availableWidth);
        int startY = centerY - Math.max(0, lines.size() - 1) * font.lineHeight / 2;
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            graphics.drawString(font, line, x + (availableWidth - font.width(line)) / 2,
                    startY + index * font.lineHeight, color, false);
        }
    }

    private void renderLeftLines(
            GuiGraphics graphics, Component text, int x, int availableWidth, int startY, int color
    ) {
        List<FormattedCharSequence> lines = font.split(text, availableWidth);
        int maximum = Math.max(1, (height - 30 - startY) / font.lineHeight);
        for (int index = 0; index < Math.min(lines.size(), maximum); index++) {
            graphics.drawString(font, lines.get(index), x, startY + index * font.lineHeight, color, false);
        }
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        if (minecraft != null && previewSound != null) minecraft.getSoundManager().stop(previewSound);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
