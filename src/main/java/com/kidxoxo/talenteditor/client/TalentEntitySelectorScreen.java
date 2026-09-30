package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Visual editor for self/event/damage-source/nearby targeting and nearby filters. */
public final class TalentEntitySelectorScreen extends Screen {
    private static final List<String> TARGETS = List.of("self", "event_entity", "damage_source", "nearby");
    private static final List<String> RELATIONS = List.of("any", "ally", "enemy");
    private static final List<String> ORDERS = List.of("nearest", "farthest", "random");
    private final TalentAdvancedActionScreen parent;
    private int targetIndex;
    private int relationIndex;
    private int orderIndex;
    private EditBox radiusField;
    private EditBox limitField;
    private Button targetButton;
    private Button relationButton;
    private Button orderButton;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private String status = "";

    TalentEntitySelectorScreen(
            TalentAdvancedActionScreen parent,
            TalentAdvancedActionScreen.SelectorDraft draft
    ) {
        super(Component.translatable("screen.talent_editor.advanced_selector.title"));
        this.parent = parent;
        targetIndex = Math.max(0, TARGETS.indexOf(draft.target()));
        relationIndex = Math.max(0, RELATIONS.indexOf(draft.relation()));
        orderIndex = Math.max(0, ORDERS.indexOf(draft.order()));
        initialRadius = draft.radius();
        initialLimit = draft.limit();
    }

    private final double initialRadius;
    private final int initialLimit;

    @Override
    protected void init() {
        int formWidth = Math.min(560, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int half = (formWidth - gap) / 2;
        targetButton = addRenderableWidget(Button.builder(Component.empty(), button ->
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        TARGETS.stream().map(value -> Component.translatable(
                                "screen.talent_editor.advanced_selector.target." + value)).toList(),
                        targetIndex, index -> { targetIndex = index; updateWidgets(); }, height))
                .bounds(x, 47, formWidth, 20).build());
        radiusField = field(x, 82, half, Double.toString(initialRadius));
        limitField = field(x + half + gap, 82, formWidth - half - gap, Integer.toString(initialLimit));
        relationButton = addRenderableWidget(Button.builder(Component.empty(), button ->
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        RELATIONS.stream().map(value -> Component.translatable(
                                "screen.talent_editor.advanced_selector.relation." + value)).toList(),
                        relationIndex, index -> { relationIndex = index; updateWidgets(); }, height))
                .bounds(x, 117, half, 20).build());
        orderButton = addRenderableWidget(Button.builder(Component.empty(), button ->
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        ORDERS.stream().map(value -> Component.translatable(
                                "screen.talent_editor.advanced_selector.order." + value)).toList(),
                        orderIndex, index -> { orderIndex = index; updateWidgets(); }, height))
                .bounds(x + half + gap, 117, formWidth - half - gap, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> apply())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
        updateWidgets();
    }

    private EditBox field(int x, int y, int width, String value) {
        EditBox result = new EditBox(font, x, y, width, 20, Component.empty());
        result.setMaxLength(16);
        result.setValue(value);
        result.setResponder(ignored -> status = "");
        addRenderableWidget(result);
        return result;
    }

    private void updateWidgets() {
        if (targetButton == null) return;
        String target = TARGETS.get(targetIndex);
        targetButton.setMessage(Component.translatable("screen.talent_editor.advanced_selector.target",
                Component.translatable("screen.talent_editor.advanced_selector.target." + target)));
        boolean nearby = target.equals("nearby");
        relationButton.active = nearby;
        orderButton.active = nearby;
        limitField.setEditable(nearby);
        relationButton.setMessage(Component.translatable("screen.talent_editor.advanced_selector.relation",
                Component.translatable("screen.talent_editor.advanced_selector.relation."
                        + RELATIONS.get(relationIndex))));
        orderButton.setMessage(Component.translatable("screen.talent_editor.advanced_selector.order",
                Component.translatable("screen.talent_editor.advanced_selector.order." + ORDERS.get(orderIndex))));
    }

    private void apply() {
        try {
            double radius = Double.parseDouble(radiusField.getValue().strip());
            int limit = Integer.parseInt(limitField.getValue().strip());
            if (!Double.isFinite(radius) || radius < 0.5D || radius > 64.0D || limit < 1 || limit > 256) {
                throw new NumberFormatException();
            }
            parent.applySelector(new TalentAdvancedActionScreen.SelectorDraft(
                    TARGETS.get(targetIndex), radius, RELATIONS.get(relationIndex), ORDERS.get(orderIndex), limit
            ));
            returnParent();
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.advanced_selector.invalid").getString();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(560, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 28, formWidth + 14, 116);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawString(font, Component.translatable("screen.talent_editor.advanced_selector.radius"),
                x, 70, EditorTheme.MUTED, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.advanced_selector.limit"),
                x + (formWidth - 6) / 2 + 6, 70, EditorTheme.MUTED, false);
        graphics.drawWordWrap(font, Component.translatable("screen.talent_editor.advanced_selector.help"),
                x, 151, formWidth, EditorTheme.MUTED);
        if (!status.isBlank()) graphics.drawString(font, status, x, height - 47, 0xFF7777, false);
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

    private void returnParent() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { returnParent(); }
}
