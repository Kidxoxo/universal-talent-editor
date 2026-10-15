package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.NumericExpression;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Visual editor for expression_compare trigger conditions. */
public final class TalentExpressionConditionScreen extends EditorScreen {
    private static final List<String> OPERATORS = List.of("lt", "lte", "eq", "neq", "gte", "gt");
    private final Screen returnScreen;
    private final TalentEditScreen owner;
    private final int triggerRawIndex;
    private final boolean replacingOtherCondition;
    private EditBox left;
    private EditBox right;
    private int operatorIndex;
    private Button operatorButton;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private String status = "";

    TalentExpressionConditionScreen(Screen returnScreen, TalentEditScreen owner, int triggerRawIndex) {
        super(Component.translatable("screen.talent_editor.expression_condition.title"));
        this.returnScreen = returnScreen;
        this.owner = owner;
        this.triggerRawIndex = triggerRawIndex;
        TalentDraft.ExpressionConditionDraft current = owner.draft().triggerExpressionCondition(triggerRawIndex);
        replacingOtherCondition = current.hadCondition() && !current.expression();
        operatorIndex = Math.max(0, OPERATORS.indexOf(current.operator()));
    }

    @Override
    protected void init() {
        TalentDraft.ExpressionConditionDraft current = owner.draft().triggerExpressionCondition(triggerRawIndex);
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        left = field(x, 61, formWidth, current.expression() ? current.left() : "0");
        operatorButton = addRenderableWidget(EditorWidgets.button(operatorLabel(), button ->
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        OPERATORS.stream().map(value -> Component.translatable(
                                "screen.talent_editor.expression_condition.operator_help." + value)).toList(),
                        operatorIndex, index -> {
                            operatorIndex = index;
                            operatorButton.setMessage(operatorLabel());
                        }, height)).bounds(x, 101, formWidth, 20).build());
        right = field(x, 141, formWidth, current.expression() ? current.right() : "0");
        int third = (formWidth - 12) / 3;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, third, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.expression_condition.clear"), button -> clear())
                .bounds(x + third + 6, height - 28, third, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), button -> apply())
                .bounds(x + (third + 6) * 2, height - 28,
                        formWidth - (third + 6) * 2, 20).build());
    }

    private EditBox field(int x, int y, int width, String value) {
        EditBox result = new EditBox(font, x, y, width, 20, Component.empty());
        result.setMaxLength(512);
        result.setValue(value);
        result.setResponder(ignored -> status = "");
        addRenderableWidget(result);
        return result;
    }

    private Component operatorLabel() {
        return Component.translatable("screen.talent_editor.expression_condition.operator",
                Component.translatable("screen.talent_editor.expression_condition.operator_help."
                        + OPERATORS.get(operatorIndex)));
    }

    private void apply() {
        try {
            String leftValue = left.getValue().strip();
            String rightValue = right.getValue().strip();
            NumericExpression.compile(leftValue);
            NumericExpression.compile(rightValue);
            owner.draft().updateTriggerExpressionCondition(
                    triggerRawIndex, leftValue, OPERATORS.get(operatorIndex), rightValue
            );
            ClientEditorState.clearValidation();
            returnParent();
        } catch (IllegalArgumentException exception) {
            status = exception.getMessage() == null ? "Invalid expression" : exception.getMessage();
        }
    }

    private void clear() {
        owner.draft().clearTriggerCondition(triggerRawIndex);
        ClientEditorState.clearValidation();
        returnParent();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, 151);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        label(graphics, "left", x, 48);
        label(graphics, "right", x, 128);
        graphics.drawWordWrap(font, Component.translatable(
                        replacingOtherCondition
                                ? "screen.talent_editor.expression_condition.replace_warning"
                                : "screen.talent_editor.expression_condition.help"),
                x, 185, formWidth, replacingOtherCondition ? 0xFFAA55 : EditorTheme.MUTED);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                x, height - 47, EditorTheme.ERROR, false);
        options.render(graphics, font, width, height);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.expression_condition." + key), x, y, EditorTheme.MUTED, false);
    }

    private void returnParent() {
        if (returnScreen instanceof TalentTriggerScreen triggerScreen) triggerScreen.refreshAdvancedChanges();
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }
    @Override public void onClose() { returnParent(); }
}
