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
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TalentDrawScreen extends Screen {
    private final DrawSessionView view;
    private int selectedIndex = -1;
    private Button confirmButton;
    private Button refreshButton;
    private Button effectsButton;
    private boolean showGeneratedEffects;
    private String status = "";
    private final int[] cardScroll;

    public TalentDrawScreen(DrawSessionView view) {
        super(Component.translatable("screen.talent_editor.draw.title"));
        this.view = view;
        this.cardScroll = new int[view.offers().size()];
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(64, Math.min(112, (width - 38) / 4));
        int total = buttonWidth * 4 + 18;
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
        effectsButton = addRenderableWidget(Button.builder(effectsLabel(), button -> {
            showGeneratedEffects = !showGeneratedEffects;
            effectsButton.setMessage(effectsLabel());
        }).bounds(x + (buttonWidth + 6) * 2, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + (buttonWidth + 6) * 3, y, buttonWidth, 20).build());
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
            int bodyTop = hasIcon ? top + 47 + iconSize : top + 46;
            List<CardLine> lines = cardLines(talent, cardWidth - 8);
            int bodyHeight = Math.max(1, bottom - bodyTop - 3);
            int contentHeight = lines.size() * 11;
            int maximumScroll = Math.max(0, contentHeight - bodyHeight);
            cardScroll[index] = Math.min(cardScroll[index], maximumScroll);
            graphics.enableScissor(x + 3, bodyTop, right - 3, bottom - 3);
            int y = bodyTop - cardScroll[index];
            for (CardLine line : lines) {
                if (y + 9 >= bodyTop && y < bottom) {
                    graphics.drawCenteredString(font, line.text(), (x + right) / 2, y, line.color());
                }
                y += 11;
            }
            graphics.disableScissor();
            if (maximumScroll > 0) renderCardScrollbar(
                    graphics, right - 3, bodyTop, bodyHeight, contentHeight, cardScroll[index], maximumScroll
            );
        }
    }

    private List<CardLine> cardLines(PlayerTalentState.TalentView talent, int width) {
        ArrayList<CardLine> lines = new ArrayList<>();
        for (PlayerTalentState.StyledTextView paragraph : talent.description()) {
            for (FormattedCharSequence line : font.split(paragraph.component(0xD0D0D0), Math.max(10, width))) {
                lines.add(new CardLine(line, 0xD0D0D0));
            }
        }
        if (showGeneratedEffects && talent.showGeneratedEffects()) {
            for (PlayerTalentState.AttributeEffectView effect : talent.attributeEffects()) {
                if (!effect.showInDescription()) continue;
                String value = effect.formula().isBlank() ? signed(effect.amount())
                        : Component.translatable("screen.talent_editor.book.dynamic_effect").getString();
                var attribute = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(effect.attribute());
                String name = attribute == null
                        ? Component.translatable("screen.talent_editor.book.unknown_attribute").getString()
                        : Component.translatable(attribute.getDescriptionId()).getString();
                Component text = Component.literal(name + " " + value);
                for (FormattedCharSequence line : font.split(text, Math.max(10, width))) {
                    lines.add(new CardLine(line, 0x9FE59F));
                }
            }
        }
        return lines;
    }

    private static void renderCardScrollbar(
            GuiGraphics graphics, int x, int y, int height, int contentHeight, int scroll, int maximumScroll
    ) {
        graphics.fill(x, y, x + 2, y + height, 0x40202020);
        int thumbHeight = Math.max(8, Math.min(height, height * height / Math.max(1, contentHeight)));
        int thumbY = y + (height - thumbHeight) * scroll / Math.max(1, maximumScroll);
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xB0A0A0A0);
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int index = cardAt(mouseX, mouseY);
        if (index < 0) return super.mouseScrolled(mouseX, mouseY, delta);
        int count = view.offers().size();
        int gap = 7;
        int cardWidth = Math.max(42, (width - 24 - gap * (count - 1)) / Math.max(1, count));
        int top = 63;
        int bottom = height - 39;
        PlayerTalentState.TalentView talent = view.offers().get(index);
        boolean hasIcon = talent.icon().isPresent();
        int iconSize = hasIcon ? fittedIconSize(view.iconSize(), cardWidth, bottom - top) : 0;
        int bodyTop = hasIcon ? top + 47 + iconSize : top + 46;
        int bodyHeight = Math.max(1, bottom - bodyTop - 3);
        int maximumScroll = Math.max(0, cardLines(talent, cardWidth - 8).size() * 11 - bodyHeight);
        if (maximumScroll <= 0) return false;
        cardScroll[index] = Math.max(0, Math.min(maximumScroll, cardScroll[index] - (int) Math.signum(delta) * 22));
        return true;
    }

    private int cardAt(double mouseX, double mouseY) {
        if (mouseY < 63 || mouseY >= height - 39 || view.offers().isEmpty()) return -1;
        int count = view.offers().size();
        int gap = 7;
        int cardWidth = Math.max(42, (width - 24 - gap * (count - 1)) / count);
        for (int index = 0; index < count; index++) {
            int x = 12 + index * (cardWidth + gap);
            if (mouseX >= x && mouseX < Math.min(width - 12, x + cardWidth)) return index;
        }
        return -1;
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

    private Component effectsLabel() {
        return Component.translatable(showGeneratedEffects
                ? "screen.talent_editor.draw.hide_effects"
                : "screen.talent_editor.draw.show_effects");
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

    private record CardLine(FormattedCharSequence text, int color) { }
}
