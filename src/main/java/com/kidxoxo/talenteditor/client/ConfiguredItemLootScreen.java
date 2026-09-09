package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.ConfiguredItemLootRule;
import com.kidxoxo.talenteditor.domain.ConfiguredItemLootTableMatch;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ConfiguredItemLootScreen extends Screen {
    private static final int MAX_RULES = 256;
    private static final List<ConfiguredItemLootTableMatch> TABLE_MATCHES = List.of(
            ConfiguredItemLootTableMatch.EXACT,
            ConfiguredItemLootTableMatch.PREFIX,
            ConfiguredItemLootTableMatch.REGEX
    );

    private final ConfiguredItemEditScreen parent;
    private final EditorBootstrap bootstrap;
    private final ArrayList<ConfiguredItemDraft.LootRuleDraft> rules;
    private int selected;
    private EditBox tableField;
    private EditBox chanceField;
    private EditBox minimumField;
    private EditBox maximumField;
    private Button tableMatchButton;
    private Button previousButton;
    private Button nextButton;
    private Button deleteButton;
    private Button conditionsButton;
    private int tableMatchIndex;
    private String status = "";

    public ConfiguredItemLootScreen(
            ConfiguredItemEditScreen parent,
            EditorBootstrap bootstrap,
            List<ConfiguredItemDraft.LootRuleDraft> rules
    ) {
        super(Component.translatable("screen.talent_editor.configured_item.loot.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.rules = new ArrayList<>(rules);
        this.selected = rules.isEmpty() ? -1 : 0;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(560, width - 32);
        int x = (width - formWidth) / 2;
        int matchWidth = Math.min(120, Math.max(80, formWidth / 3));
        int matchGap = 6;
        tableMatchButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleTableMatch())
                .bounds(x, 43, matchWidth, 20).build());
        tableField = field(x + matchWidth + matchGap, 43,
                formWidth - matchWidth - matchGap, ConfiguredItemLootRule.MAX_TABLE_EXPRESSION_LENGTH);
        int gap = 6;
        int chanceWidth = (formWidth - gap * 2) / 3;
        chanceField = field(x, 84, chanceWidth, 32);
        minimumField = field(x + chanceWidth + gap, 84, chanceWidth, 4);
        maximumField = field(x + (chanceWidth + gap) * 2, 84,
                formWidth - (chanceWidth + gap) * 2, 4);

        int actionWidth = (formWidth - gap * 3) / 4;
        previousButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.previous"), button -> switchRule(-1))
                .bounds(x, 116, actionWidth, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.next"), button -> switchRule(1))
                .bounds(x + actionWidth + gap, 116, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.add"), button -> addRule())
                .bounds(x + (actionWidth + gap) * 2, 116, actionWidth, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.delete"), button -> deleteRule())
                .bounds(x + (actionWidth + gap) * 3, 116,
                        formWidth - (actionWidth + gap) * 3, 20).build());
        conditionsButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.conditions"), button -> openConditions())
                .bounds(x, 146, formWidth, 20).build());

        int half = (formWidth - gap) / 2;
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.apply"), button -> apply())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x + half + gap, height - 28, formWidth - half - gap, 20).build());
        loadSelected();
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
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(560, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int third = (formWidth - gap * 2) / 3;
        int matchWidth = Math.min(120, Math.max(80, formWidth / 3));
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.loot.current",
                selected < 0 ? 0 : selected + 1, rules.size()
        ), x, 25, 0xA0A0A0, false);
        label(graphics, "screen.talent_editor.configured_item.loot.match", x, 32);
        label(graphics, "screen.talent_editor.configured_item.loot.table_expression",
                x + matchWidth + gap, 32);
        label(graphics, "screen.talent_editor.configured_item.loot.chance", x, 73);
        label(graphics, "screen.talent_editor.configured_item.loot.minimum", x + third + gap, 73);
        label(graphics, "screen.talent_editor.configured_item.loot.maximum", x + (third + gap) * 2, 73);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                "screen.talent_editor.configured_item.loot.help").getString(), formWidth),
                x, 176, 0x808080, false);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth), x,
                    195, 0xFF7777, false);
        }
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    private void switchRule(int direction) {
        if (!saveSelected()) return;
        selected = Math.max(0, Math.min(selected + direction, rules.size() - 1));
        loadSelected();
    }

    private void addRule() {
        if (!saveSelected()) return;
        if (rules.size() >= MAX_RULES) {
            status = Component.translatable("screen.talent_editor.configured_item.loot.limit").getString();
            return;
        }
        String table = rules.stream().noneMatch(rule ->
                rule.table().equals("minecraft:chests/simple_dungeon"))
                ? "minecraft:chests/simple_dungeon"
                : "custom:chests/new_rule_" + (rules.size() + 1);
        rules.add(ConfiguredItemDraft.LootRuleDraft.create(table));
        selected = rules.size() - 1;
        loadSelected();
    }

    private void cycleTableMatch() {
        tableMatchIndex = (tableMatchIndex + 1) % TABLE_MATCHES.size();
        updateTableMatchLabel();
        status = "";
    }

    private void deleteRule() {
        if (selected < 0) return;
        rules.remove(selected);
        selected = rules.isEmpty() ? -1 : Math.min(selected, rules.size() - 1);
        loadSelected();
    }

    private void openConditions() {
        if (minecraft == null || selected < 0 || !saveSelected()) return;
        minecraft.setScreen(new ConfiguredItemLootConditionScreen(
                this, bootstrap, rules.get(selected)
        ));
    }

    boolean replaceSelectedConditions(ConfiguredItemDraft.LootRuleDraft rule) {
        if (selected < 0 || selected >= rules.size()) return false;
        for (int index = 0; index < rules.size(); index++) {
            if (index != selected && rules.get(index).sameScope(rule)) {
                status = Component.translatable(
                        "screen.talent_editor.configured_item.loot.duplicate_scope").getString();
                return false;
            }
        }
        rules.set(selected, rule);
        status = "";
        return true;
    }

    private boolean saveSelected() {
        if (selected < 0) return true;
        String rawTable = tableField.getValue().strip();
        ConfiguredItemLootTableMatch tableMatch = TABLE_MATCHES.get(tableMatchIndex);
        double chance;
        int minimum;
        int maximum;
        try {
            chance = Double.parseDouble(chanceField.getValue().strip());
            minimum = Integer.parseInt(minimumField.getValue().strip());
            maximum = Integer.parseInt(maximumField.getValue().strip());
        } catch (NumberFormatException ignored) {
            return invalid("screen.talent_editor.configured_item.loot.invalid_numbers");
        }
        try {
            ConfiguredItemLootRule.validateTableExpression(tableMatch, rawTable);
        } catch (IllegalArgumentException ignored) {
            return invalid(tableMatch == ConfiguredItemLootTableMatch.REGEX
                    ? "screen.talent_editor.configured_item.loot.invalid_regex"
                    : "screen.talent_editor.configured_item.loot.invalid_table");
        }
        if (!Double.isFinite(chance) || chance < 0.0D || chance > 1.0D
                || minimum < 1 || maximum < minimum || maximum > 64) {
            return invalid("screen.talent_editor.configured_item.loot.invalid_numbers");
        }
        for (int index = 0; index < rules.size(); index++) {
            ConfiguredItemDraft.LootRuleDraft candidate = rules.get(selected).withValues(
                    tableMatch.serializedName(), rawTable, chance, minimum, maximum
            );
            if (index != selected && rules.get(index).sameScope(candidate)) {
                return invalid("screen.talent_editor.configured_item.loot.duplicate_table");
            }
        }
        rules.set(selected, rules.get(selected).withValues(
                tableMatch.serializedName(), rawTable, chance, minimum, maximum
        ));
        return true;
    }

    private boolean invalid(String key) {
        status = Component.translatable(key).getString();
        return false;
    }

    private void loadSelected() {
        boolean active = selected >= 0;
        tableField.setEditable(active);
        chanceField.setEditable(active);
        minimumField.setEditable(active);
        maximumField.setEditable(active);
        tableMatchButton.active = active;
        deleteButton.active = active;
        conditionsButton.active = active;
        previousButton.active = active && selected > 0;
        nextButton.active = active && selected + 1 < rules.size();
        if (!active) {
            tableMatchIndex = 0;
            updateTableMatchLabel();
            tableField.setValue("");
            chanceField.setValue("");
            minimumField.setValue("");
            maximumField.setValue("");
            return;
        }
        ConfiguredItemDraft.LootRuleDraft rule = rules.get(selected);
        tableMatchIndex = Math.max(0, TABLE_MATCHES.stream()
                .map(ConfiguredItemLootTableMatch::serializedName).toList().indexOf(rule.tableMatch()));
        updateTableMatchLabel();
        tableField.setValue(rule.table());
        chanceField.setValue(formatDecimal(rule.chance()));
        minimumField.setValue(Integer.toString(rule.minCount()));
        maximumField.setValue(Integer.toString(rule.maxCount()));
        status = "";
    }

    private void updateTableMatchLabel() {
        if (tableMatchButton == null) return;
        tableMatchButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.loot.match."
                        + TABLE_MATCHES.get(tableMatchIndex).serializedName()
        ));
    }

    private void apply() {
        if (!saveSelected()) return;
        parent.updateLootRules(List.copyOf(rules));
        returnParent();
    }

    private static String formatDecimal(double value) {
        if (!Double.isFinite(value)) return Double.toString(value);
        return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
