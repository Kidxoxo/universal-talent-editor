package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ConfiguredItemUseScreen extends Screen {
    private static final int MAX_ACTIONS = 256;

    private final ConfiguredItemEditScreen parent;
    private final EditorBootstrap bootstrap;
    private final boolean editable;
    private final ArrayList<ConfiguredItemDraft.UseActionDraft> actions;
    private boolean enabled;
    private int selected;
    private EditBox cooldownField;
    private EditBox consumeField;
    private Button enabledButton;
    private Button previousButton;
    private Button nextButton;
    private Button deleteButton;
    private Button editButton;
    private Button upButton;
    private Button downButton;
    private Button applyButton;
    private String status = "";

    ConfiguredItemUseScreen(
            ConfiguredItemEditScreen parent,
            EditorBootstrap bootstrap,
            ConfiguredItemDraft.UseDraft draft
    ) {
        super(Component.translatable("screen.talent_editor.configured_item.use.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.editable = draft.editable();
        this.enabled = draft.enabled();
        this.actions = new ArrayList<>(draft.actions());
        this.selected = actions.isEmpty() ? -1 : 0;
        this.initialCooldown = draft.cooldownTicks();
        this.initialConsume = draft.consumeCount();
    }

    private final int initialCooldown;
    private final int initialConsume;

    @Override
    protected void init() {
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int third = (formWidth - gap * 2) / 3;
        enabledButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            enabled = !enabled;
            updateButtons();
        }).bounds(x, 42, third, 20).build());
        cooldownField = field(x + third + gap, 42, third, 10);
        consumeField = field(x + (third + gap) * 2, 42,
                formWidth - (third + gap) * 2, 3);
        cooldownField.setValue(Integer.toString(initialCooldown));
        consumeField.setValue(Integer.toString(initialConsume));

        int quarter = (formWidth - gap * 3) / 4;
        previousButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.previous"), button -> select(-1))
                .bounds(x, 84, quarter, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.next"), button -> select(1))
                .bounds(x + quarter + gap, 84, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.add"), button -> addAction())
                .bounds(x + (quarter + gap) * 2, 84, quarter, 20).build()).active = editable;
        deleteButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.delete"), button -> deleteAction())
                .bounds(x + (quarter + gap) * 3, 84,
                        formWidth - (quarter + gap) * 3, 20).build());

        int actionWidth = (formWidth - gap * 2) / 3;
        editButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.edit"), button -> editAction())
                .bounds(x, 116, actionWidth, 20).build());
        upButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.up"), button -> move(-1))
                .bounds(x + actionWidth + gap, 116, actionWidth, 20).build());
        downButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.down"), button -> move(1))
                .bounds(x + (actionWidth + gap) * 2, 116,
                        formWidth - (actionWidth + gap) * 2, 20).build());

        int half = (formWidth - gap) / 2;
        applyButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.apply"), button -> apply())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x + half + gap, height - 28, formWidth - half - gap, 20).build());
        updateButtons();
    }

    private EditBox field(int x, int y, int width, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setResponder(ignored -> status = "");
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 8, 26, formWidth + 16, 190);
        super.render(graphics, mouseX, mouseY, partialTick);
        int gap = 6;
        int third = (formWidth - gap * 2) / 3;
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        label(graphics, "enabled", x, 31);
        label(graphics, "cooldown", x + third + gap, 31);
        label(graphics, "consume", x + (third + gap) * 2, 31);
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.use.current",
                selected < 0 ? 0 : selected + 1, actions.size()
        ), x, 72, 0xA0A0A0, false);
        String summary = selected < 0 ? Component.translatable(
                "screen.talent_editor.configured_item.use.no_action").getString()
                : Component.translatable("screen.talent_editor.configured_item.use.action."
                        + actions.get(selected).type().serializedName()).getString();
        graphics.drawCenteredString(font, summary, width / 2, 151, 0xFFFFFF);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                "screen.talent_editor.configured_item.use.help").getString(), formWidth),
                x, 174, 0x808080, false);
        if (!editable) {
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                    "screen.talent_editor.configured_item.use.advanced_read_only").getString(), formWidth),
                    x, 195, 0xFFFFAA55, false);
        } else if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, 195, 0xFF7777, false);
        }
    }

    private void label(GuiGraphics graphics, String suffix, int x, int y) {
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.use." + suffix), x, y, 0xA0A0A0, false);
    }

    private void select(int direction) {
        if (actions.isEmpty()) return;
        selected = Math.max(0, Math.min(selected + direction, actions.size() - 1));
        updateButtons();
    }

    private void addAction() {
        if (!editable) return;
        if (actions.size() >= MAX_ACTIONS) {
            status = Component.translatable("screen.talent_editor.configured_item.use.limit").getString();
            return;
        }
        actions.add(ConfiguredItemDraft.UseActionDraft.defaults(
                ConfiguredItemDraft.UseActionType.ADD_EFFECT
        ));
        selected = actions.size() - 1;
        updateButtons();
        editAction();
    }

    private void deleteAction() {
        if (!editable || selected < 0) return;
        actions.remove(selected);
        selected = actions.isEmpty() ? -1 : Math.min(selected, actions.size() - 1);
        updateButtons();
    }

    private void move(int direction) {
        if (!editable || selected < 0) return;
        int target = selected + direction;
        if (target < 0 || target >= actions.size()) return;
        var action = actions.remove(selected);
        actions.add(target, action);
        selected = target;
        updateButtons();
    }

    private void editAction() {
        if (!editable || selected < 0 || minecraft == null) return;
        minecraft.setScreen(new ConfiguredItemUseActionScreen(
                this, bootstrap, actions.get(selected)
        ));
    }

    void replaceSelected(ConfiguredItemDraft.UseActionDraft action) {
        if (selected < 0 || selected >= actions.size()) return;
        actions.set(selected, action);
        status = "";
        updateButtons();
    }

    private void updateButtons() {
        if (enabledButton == null) return;
        enabledButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.use.enabled_value",
                Component.translatable("options." + (enabled ? "on" : "off"))
        ));
        enabledButton.active = editable;
        cooldownField.setEditable(editable);
        consumeField.setEditable(editable);
        boolean selectedAction = editable && selected >= 0;
        previousButton.active = selectedAction && selected > 0;
        nextButton.active = selectedAction && selected + 1 < actions.size();
        deleteButton.active = selectedAction;
        editButton.active = selectedAction;
        upButton.active = selectedAction && selected > 0;
        downButton.active = selectedAction && selected + 1 < actions.size();
        applyButton.active = editable;
    }

    private void apply() {
        if (!editable) return;
        int cooldown;
        int consume;
        try {
            cooldown = Integer.parseInt(cooldownField.getValue().strip());
            consume = Integer.parseInt(consumeField.getValue().strip());
        } catch (NumberFormatException ignored) {
            status = Component.translatable("screen.talent_editor.configured_item.use.invalid_numbers").getString();
            return;
        }
        if (cooldown < 0 || cooldown > 1_200_000 || consume < 0 || consume > 64) {
            status = Component.translatable("screen.talent_editor.configured_item.use.invalid_numbers").getString();
            return;
        }
        if (enabled && actions.isEmpty()) {
            status = Component.translatable("screen.talent_editor.configured_item.use.action_required").getString();
            return;
        }
        parent.updateUse(new ConfiguredItemDraft.UseDraft(
                true, enabled, cooldown, consume, List.copyOf(actions)
        ));
        returnParent();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
