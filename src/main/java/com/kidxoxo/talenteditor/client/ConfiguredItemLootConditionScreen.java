package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ConfiguredItemLootConditionScreen extends Screen {
    private static final List<String> TARGETS = List.of("this_entity", "killer_entity");

    private final ConfiguredItemLootScreen parent;
    private final EditorBootstrap bootstrap;
    private final ConfiguredItemDraft.LootRuleDraft original;
    private int targetIndex;
    private EditBox dimensionsField;
    private EditBox entityTypesField;
    private EditBox entityTagsField;
    private EditBox chancePerLuckField;
    private Button targetButton;
    private String status = "";

    ConfiguredItemLootConditionScreen(
            ConfiguredItemLootScreen parent,
            EditorBootstrap bootstrap,
            ConfiguredItemDraft.LootRuleDraft rule
    ) {
        super(Component.translatable("screen.talent_editor.configured_item.loot.conditions.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.original = rule;
        this.targetIndex = Math.max(0, TARGETS.indexOf(rule.entityTarget()));
    }

    @Override
    protected void init() {
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        dimensionsField = field(x, 38, formWidth, 2_048);
        targetButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            targetIndex = (targetIndex + 1) % TARGETS.size();
            updateTargetButton();
        }).bounds(x, 68, formWidth, 20).build());
        entityTypesField = field(x, 98, formWidth - 92, 4_096);
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.conditions.choose_type"),
                        button -> choose(ResourceReferencePickerScreen.Source.ENTITY_TYPE, entityTypesField))
                .bounds(x + formWidth - 86, 98, 86, 20).build());
        entityTagsField = field(x, 128, formWidth - 92, 4_096);
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.conditions.choose_tag"),
                        button -> choose(ResourceReferencePickerScreen.Source.ENTITY_TYPE_TAG, entityTagsField))
                .bounds(x + formWidth - 86, 128, 86, 20).build());
        chancePerLuckField = field(x, 158, formWidth, 32);

        int gap = 6;
        int half = (formWidth - gap) / 2;
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.loot.conditions.apply"), button -> apply())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x + half + gap, height - 28, formWidth - half - gap, 20).build());

        dimensionsField.setValue(join(original.dimensions()));
        entityTypesField.setValue(join(original.entityTypes()));
        entityTagsField.setValue(join(original.entityTags()));
        chancePerLuckField.setValue(formatDecimal(original.chancePerLuck()));
        updateTargetButton();
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
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 8, 27, formWidth + 16, 187);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        label(graphics, "dimensions", x, 27);
        label(graphics, "entity_target", x, 57);
        label(graphics, "entity_types", x, 87);
        label(graphics, "entity_tags", x, 117);
        label(graphics, "chance_per_luck", x, 147);
        Double factor = parseDecimal(chancePerLuckField == null ? "" : chancePerLuckField.getValue());
        if (factor != null && Double.isFinite(factor)) {
            graphics.drawString(font, Component.translatable(
                    "screen.talent_editor.configured_item.loot.conditions.preview",
                    percent(effectiveChance(original.chance(), factor, 0.0D)),
                    percent(effectiveChance(original.chance(), factor, 1.0D)),
                    percent(effectiveChance(original.chance(), factor, 10.0D))
            ), x, 183, 0x80D8FF, false);
        }
        String footer = status.isBlank()
                ? Component.translatable("screen.talent_editor.configured_item.loot.conditions.help").getString()
                : status;
        graphics.drawString(font, font.plainSubstrByWidth(footer, formWidth),
                x, Math.min(196, height - 47), status.isBlank() ? 0x808080 : 0xFF7777, false);
    }

    private void label(GuiGraphics graphics, String suffix, int x, int y) {
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.loot.conditions." + suffix), x, y, 0xA0A0A0, false);
    }

    private void updateTargetButton() {
        if (targetButton != null) targetButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.loot.conditions.target." + TARGETS.get(targetIndex)
        ));
    }

    private void choose(ResourceReferencePickerScreen.Source source, EditBox destination) {
        if (minecraft == null) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, source, bootstrap, appendTo(destination)
        ));
    }

    private Consumer<ResourceLocation> appendTo(EditBox field) {
        return id -> {
            List<ResourceLocation> current = parseIds(field.getValue());
            if (current == null || current.contains(id)) return;
            ArrayList<ResourceLocation> updated = new ArrayList<>(current);
            updated.add(id);
            field.setValue(join(updated));
        };
    }

    private void apply() {
        List<ResourceLocation> dimensions = parseIds(dimensionsField.getValue());
        List<ResourceLocation> entityTypes = parseIds(entityTypesField.getValue());
        List<ResourceLocation> entityTags = parseIds(entityTagsField.getValue());
        if (dimensions == null || entityTypes == null || entityTags == null) {
            status = Component.translatable(
                    "screen.talent_editor.configured_item.loot.conditions.invalid_ids").getString();
            return;
        }
        Double chancePerLuck = parseDecimal(chancePerLuckField.getValue());
        if (chancePerLuck == null || !Double.isFinite(chancePerLuck)
                || chancePerLuck < -1.0D || chancePerLuck > 1.0D) {
            status = Component.translatable(
                    "screen.talent_editor.configured_item.loot.conditions.invalid_luck").getString();
            return;
        }
        ConfiguredItemDraft.LootRuleDraft updated = original.withConditions(
                chancePerLuck, dimensions, TARGETS.get(targetIndex), entityTypes, entityTags
        );
        if (parent.replaceSelectedConditions(updated)) returnParent();
    }

    private static List<ResourceLocation> parseIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        ArrayList<ResourceLocation> result = new ArrayList<>();
        HashSet<ResourceLocation> seen = new HashSet<>();
        for (String part : value.split(",")) {
            String raw = part.strip().toLowerCase(Locale.ROOT);
            if (raw.isBlank()) continue;
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null || !raw.contains(":") || !seen.add(id)) return null;
            result.add(id);
        }
        return List.copyOf(result);
    }

    private static String join(List<ResourceLocation> values) {
        return values.stream().map(ResourceLocation::toString).reduce(
                (left, right) -> left + ", " + right
        ).orElse("");
    }

    private static Double parseDecimal(String value) {
        try {
            return Double.parseDouble(value.strip());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static double effectiveChance(double base, double chancePerLuck, double luck) {
        if (!Double.isFinite(luck)) luck = 0.0D;
        return Math.max(0.0D, Math.min(1.0D, base + luck * chancePerLuck));
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0D);
    }

    private static String formatDecimal(double value) {
        if (!Double.isFinite(value)) return Double.toString(value);
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
