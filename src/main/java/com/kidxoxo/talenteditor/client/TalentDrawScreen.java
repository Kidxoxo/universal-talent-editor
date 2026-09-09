package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.draw.DrawResult;
import com.kidxoxo.talenteditor.network.DrawSessionView;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.PlayerTalentState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class TalentDrawScreen extends Screen {
    private final DrawSessionView view;
    private int selectedIndex = -1;
    private Button confirmButton;
    private Button refreshButton;
    private String status = "";

    public TalentDrawScreen(DrawSessionView view) {
        super(Component.translatable("screen.talent_editor.draw.title"));
        this.view = view;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(76, Math.min(120, (width - 32) / 3));
        int total = buttonWidth * 3 + 12;
        int x = (width - total) / 2;
        int y = height - 28;
        refreshButton = addRenderableWidget(Button.builder(refreshLabel(), button -> {
            refreshButton.active = false;
            confirmButton.active = false;
            status = Component.translatable("screen.talent_editor.draw.waiting").getString();
            EditorNetwork.refreshDraw(view.sessionId());
        }).bounds(x, y, buttonWidth, 20).build());
        confirmButton = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.draw.confirm"),
                button -> confirm()).bounds(x + buttonWidth + 6, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + (buttonWidth + 6) * 2, y, buttonWidth, 20).build());
        confirmButton.active = false;
        refreshButton.active = canRefresh();
    }

    public void setResult(DrawResult.Status result) {
        status = Component.translatable(
                "screen.talent_editor.draw.result." + result.name().toLowerCase(Locale.ROOT)
        ).getString();
        confirmButton.active = selectedIndex >= 0;
        refreshButton.active = canRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(
                "screen.talent_editor.draw.summary", view.poolName(), view.drawTokens(), view.refreshesRemaining()
        ), width / 2, 23, 0xA0A0A0);
        String mechanics = Component.translatable(
                "screen.talent_editor.draw.mechanics", compact(view.luckValue()),
                view.pityAfter() <= 0 ? "-" : view.pityProgress() + "/" + view.pityAfter(),
                probabilitySummary()
        ).getString();
        graphics.drawCenteredString(font, font.plainSubstrByWidth(mechanics, width - 24),
                width / 2, 36, view.pityActive() ? 0xFFD955 : 0x808080);
        if (!status.isBlank()) graphics.drawCenteredString(font, status, width / 2, 48, 0xFFAA55);
        renderCards(graphics, mouseX, mouseY);
    }

    private void renderCards(GuiGraphics graphics, int mouseX, int mouseY) {
        int count = Math.max(1, view.offers().size());
        int gap = 7;
        int left = 12;
        int available = width - 24 - gap * (count - 1);
        int cardWidth = Math.max(42, available / count);
        int top = 63;
        int bottom = height - 39;
        for (int index = 0; index < view.offers().size(); index++) {
            PlayerTalentState.TalentView talent = view.offers().get(index);
            int x = left + index * (cardWidth + gap);
            int right = Math.min(width - 12, x + cardWidth);
            boolean hovered = mouseX >= x && mouseX < right && mouseY >= top && mouseY < bottom;
            int rarity = 0xFF000000 | (talent.rarityColor() & 0xFFFFFF);
            int background = index == selectedIndex ? 0x8050A0E8 : hovered ? 0x504A90E2 : 0x30202020;
            graphics.fill(x, top, right, bottom, background);
            graphics.fill(x, top, right, top + 3, rarity);
            boolean hasIcon = talent.icon().isPresent();
            int iconSize = hasIcon ? fittedIconSize(view.iconSize(), right - x, bottom - top) : 0;
            if (hasIcon) {
                renderIcon(graphics, talent.icon().orElseThrow(), (x + right - iconSize) / 2, top + 10, iconSize);
            }
            graphics.drawCenteredString(font, fit(talent.name(), cardWidth - 8, rarity),
                    (x + right) / 2, hasIcon ? top + 15 + iconSize : top + 12, rarity);
            graphics.drawCenteredString(font, fit(talent.rarityName(), cardWidth - 8, 0xA0A0A0),
                    (x + right) / 2, hasIcon ? top + 29 + iconSize : top + 27, 0xA0A0A0);
            int y = hasIcon ? top + 47 + iconSize : top + 46;
            for (PlayerTalentState.StyledTextView paragraph : talent.description()) {
                if (y > bottom - 14) break;
                graphics.drawCenteredString(font, fit(paragraph, cardWidth - 8, 0xD0D0D0),
                        (x + right) / 2, y, 0xD0D0D0);
                y += 12;
            }
            if (talent.showGeneratedEffects()) {
                for (PlayerTalentState.AttributeEffectView effect : talent.attributeEffects()) {
                    if (!effect.showInDescription() || y > bottom - 14) continue;
                    String value = effect.formula().isBlank() ? signed(effect.amount()) : effect.formula();
                    String effectText = shortAttribute(effect.attribute().toString()) + " " + value;
                    graphics.drawCenteredString(font, fit(effectText, cardWidth - 8),
                            (x + right) / 2, y, 0x9FE59F);
                    y += 12;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= 63 && mouseY < height - 39 && !view.offers().isEmpty()) {
            int count = view.offers().size();
            int gap = 7;
            int cardWidth = Math.max(42, (width - 24 - gap * (count - 1)) / count);
            for (int index = 0; index < count; index++) {
                int x = 12 + index * (cardWidth + gap);
                if (mouseX >= x && mouseX < Math.min(width - 12, x + cardWidth)) {
                    selectedIndex = index;
                    confirmButton.active = true;
                    status = "";
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void confirm() {
        if (selectedIndex < 0 || selectedIndex >= view.offers().size()) return;
        confirmButton.active = false;
        refreshButton.active = false;
        status = Component.translatable("screen.talent_editor.draw.waiting").getString();
        EditorNetwork.selectDraw(view.sessionId(), view.offers().get(selectedIndex).id());
    }

    private Component refreshLabel() {
        return view.refreshesRemaining() > 0
                ? Component.translatable("screen.talent_editor.draw.refresh", view.refreshesRemaining())
                : Component.translatable("screen.talent_editor.draw.refresh_paid");
    }

    private boolean canRefresh() {
        return view.refreshesRemaining() > 0 || view.drawTokens() >= 2;
    }

    private String fit(String value, int maxWidth) {
        return font.plainSubstrByWidth(value, Math.max(10, maxWidth));
    }

    private Component fit(PlayerTalentState.StyledTextView value, int maxWidth, int fallbackColor) {
        Component resolved = value.component(fallbackColor);
        return Component.literal(font.plainSubstrByWidth(resolved.getString(), Math.max(10, maxWidth)))
                .setStyle(resolved.getStyle());
    }

    private static String shortAttribute(String value) {
        int marker = value.lastIndexOf('.');
        return marker >= 0 ? value.substring(marker + 1) : value;
    }

    private static String signed(double value) {
        String compact = value == Math.rint(value)
                ? Long.toString((long) value)
                : String.format(Locale.ROOT, "%.4f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
        return value >= 0 ? "+" + compact : compact;
    }

    private String probabilitySummary() {
        if (view.rarityProbabilities().isEmpty()) return "-";
        return view.rarityProbabilities().entrySet().stream()
                .map(entry -> shortAttribute(entry.getKey().toString()) + " "
                        + String.format(Locale.ROOT, "%.1f%%", entry.getValue() * 100.0D))
                .reduce((left, right) -> left + " · " + right)
                .orElse("-");
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.3f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }

    static int fittedIconSize(int configuredSize, int cardWidth, int cardHeight) {
        int widthLimit = Math.max(1, cardWidth - 8);
        int heightLimit = Math.max(1, cardHeight - 61);
        return Math.max(1, Math.min(configuredSize, Math.min(widthLimit, heightLimit)));
    }

    private static void renderIcon(GuiGraphics graphics, ResourceLocation icon, int x, int y, int size) {
        float scale = size / 16.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0D);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(icon, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.pose().popPose();
    }
}
