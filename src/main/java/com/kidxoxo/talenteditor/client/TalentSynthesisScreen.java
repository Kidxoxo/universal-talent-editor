package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public final class TalentSynthesisScreen extends Screen {
    private final TalentEditScreen parent;
    private final Screen returnScreen;
    private boolean enabled;
    private Button enabledButton;
    private EditBox talentsField;
    private EditBox countersField;
    private EditBox itemsField;
    private String talentsValue;
    private String countersValue;
    private String itemsValue;
    private String status = "";

    public TalentSynthesisScreen(TalentEditScreen parent) {
        this(parent, parent);
    }

    TalentSynthesisScreen(TalentEditScreen parent, Screen returnScreen) {
        super(Component.translatable("screen.talent_editor.synthesis.title"));
        this.parent = parent;
        this.returnScreen = returnScreen;
        TalentDraft.SynthesisDraft synthesis = parent.synthesis();
        this.enabled = synthesis.enabled();
        this.talentsValue = String.join(", ", synthesis.consumeTalents().stream()
                .map(ResourceLocation::toString).toList());
        this.countersValue = formatCosts(synthesis.consumeCounters());
        this.itemsValue = formatCosts(synthesis.consumeItems());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(540, width - 32);
        int x = (width - formWidth) / 2;
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            enabledButton.setMessage(enabledLabel());
        }).bounds(x, 42, formWidth, 20).build());

        int chooseWidth = Math.min(84, Math.max(62, formWidth / 5));
        int fieldWidth = formWidth - chooseWidth - 6;
        talentsField = field(x, 83, fieldWidth, "screen.talent_editor.synthesis.talents",
                talentsValue, value -> talentsValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.TALENT, this::chooseTalent))
                .bounds(x + fieldWidth + 6, 83, chooseWidth, 20).build());
        countersField = field(x, 124, fieldWidth, "screen.talent_editor.synthesis.counters",
                countersValue, value -> countersValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.COUNTER, this::chooseCounter))
                .bounds(x + fieldWidth + 6, 124, chooseWidth, 20).build());
        itemsField = field(x, 165, fieldWidth, "screen.talent_editor.synthesis.items",
                itemsValue, value -> itemsValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.ITEM, this::chooseItem))
                .bounds(x + fieldWidth + 6, 165, chooseWidth, 20).build());

        int half = (formWidth - 6) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.synthesis.apply"),
                        button -> apply())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x + half + 6, height - 28, formWidth - half - 6, 20).build());
    }

    private EditBox field(int x, int y, int width, String key, String value,
                          java.util.function.Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.translatable(key));
        field.setMaxLength(4_096);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
            status = "";
        });
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(540, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 8, 31, formWidth + 16, 178);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 13, EditorTheme.TEXT);
        label(graphics, "screen.talent_editor.synthesis.talents", x, 72);
        label(graphics, "screen.talent_editor.synthesis.counters", x, 113);
        label(graphics, "screen.talent_editor.synthesis.items", x, 154);
        if (status.isBlank()) {
            ReferenceDiagnostics.Summary references = ReferenceDiagnostics.summarize(
                    parent.bootstrap(), ResourceReferencePickerScreen.Source.TALENT, talentsValue, false
            ).plus(ReferenceDiagnostics.summarize(
                    parent.bootstrap(), ResourceReferencePickerScreen.Source.COUNTER, countersValue, true
            )).plus(ReferenceDiagnostics.summarize(
                    parent.bootstrap(), ResourceReferencePickerScreen.Source.ITEM, itemsValue, true
            ));
            graphics.drawString(font, font.plainSubstrByWidth(
                    ReferenceDiagnostics.label(references).getString(), formWidth
            ), x, 193, ReferenceDiagnostics.color(references), false);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth), x,
                    height - 47, 0xFF7777, false);
        }
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    private void apply() {
        LinkedHashSet<ResourceLocation> talents = new LinkedHashSet<>();
        for (String raw : talentsField.getValue().split(",")) {
            if (raw.isBlank()) continue;
            ResourceLocation id = ResourceLocation.tryParse(raw.strip());
            if (id == null || !talents.add(id)) {
                status = Component.translatable("screen.talent_editor.synthesis.invalid_talent", raw.strip()).getString();
                return;
            }
        }
        Map<ResourceLocation, Long> counters = parseCosts(
                countersField.getValue(), 1_000_000_000_000L,
                "screen.talent_editor.synthesis.invalid_counter"
        );
        if (counters == null) return;
        Map<ResourceLocation, Long> rawItems = parseCosts(
                itemsField.getValue(), 1_000_000L,
                "screen.talent_editor.synthesis.invalid_item"
        );
        if (rawItems == null) return;
        LinkedHashMap<ResourceLocation, Integer> items = new LinkedHashMap<>();
        rawItems.forEach((id, count) -> items.put(id, count.intValue()));
        parent.updateSynthesis(enabled, new ArrayList<>(talents), counters, items);
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }

    private Map<ResourceLocation, Long> parseCosts(String value, long maximum, String errorKey) {
        LinkedHashMap<ResourceLocation, Long> result = new LinkedHashMap<>();
        for (String raw : value.split(",")) {
            if (raw.isBlank()) continue;
            String[] parts = raw.strip().split("=", -1);
            ResourceLocation id = parts.length == 2 ? ResourceLocation.tryParse(parts[0].strip()) : null;
            long count;
            try {
                count = parts.length == 2 ? Long.parseLong(parts[1].strip()) : 0L;
            } catch (NumberFormatException ignored) {
                count = 0L;
            }
            if (id == null || count < 1L || count > maximum || result.putIfAbsent(id, count) != null) {
                status = Component.translatable(errorKey, raw.strip()).getString();
                return null;
            }
        }
        return Map.copyOf(result);
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.synthesis.enabled_yes"
                : "screen.talent_editor.synthesis.enabled_no");
    }

    private void openPicker(
            ResourceReferencePickerScreen.Source source,
            java.util.function.Consumer<ResourceLocation> selection
    ) {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(this, source, parent.bootstrap(), selection));
        }
    }

    private void chooseTalent(ResourceLocation id) {
        talentsValue = appendUnique(talentsValue, id, false);
    }

    private void chooseCounter(ResourceLocation id) {
        countersValue = appendUnique(countersValue, id, true);
    }

    private void chooseItem(ResourceLocation id) {
        itemsValue = appendUnique(itemsValue, id, true);
    }

    static String appendUnique(String value, ResourceLocation id, boolean withCost) {
        for (String raw : value.split(",")) {
            String entry = raw.strip();
            int equals = entry.indexOf('=');
            String key = equals < 0 ? entry : entry.substring(0, equals).strip();
            if (key.equals(id.toString())) return value;
        }
        String addition = id + (withCost ? "=1" : "");
        return value.isBlank() ? addition : value.strip() + ", " + addition;
    }

    private static String formatCosts(Map<ResourceLocation, ? extends Number> costs) {
        return String.join(", ", costs.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }
}
