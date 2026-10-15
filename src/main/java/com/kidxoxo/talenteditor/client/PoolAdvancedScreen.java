package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class PoolAdvancedScreen extends EditorScreen {
    private static final List<String> POLICIES = List.of("exclude", "refund_token", "add_counter");
    private final PoolEditScreen parent;
    private final EditorBootstrap bootstrap;
    private String policy;
    private EditBox luckAttribute;
    private EditBox sampleLuck;
    private EditBox luckFactors;
    private EditBox pityRarity;
    private EditBox pityAfter;
    private EditBox duplicateCounter;
    private EditBox duplicateAmount;
    private Button policyButton;
    private String luckAttributeValue;
    private String sampleLuckValue;
    private String luckFactorsValue;
    private String pityRarityValue;
    private String pityAfterValue;
    private String duplicateCounterValue;
    private String duplicateAmountValue;
    private String status = "";
    private final EditorOptionPopup options = new EditorOptionPopup();

    public PoolAdvancedScreen(PoolEditScreen parent, EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.pool.advanced.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        PoolDraft.Advanced advanced = parent.advanced();
        this.policy = POLICIES.contains(advanced.duplicatePolicy())
                ? advanced.duplicatePolicy() : "exclude";
        this.luckAttributeValue = advanced.luckAttribute().map(ResourceLocation::toString).orElse("");
        this.sampleLuckValue = "0";
        this.luckFactorsValue = formatMap(advanced.luckFactors());
        this.pityRarityValue = advanced.pityRarity().map(ResourceLocation::toString).orElse("");
        this.pityAfterValue = Integer.toString(advanced.pityAfter());
        this.duplicateCounterValue = advanced.duplicateCounter().map(ResourceLocation::toString).orElse("");
        this.duplicateAmountValue = Long.toString(advanced.duplicateCounterAmount());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int half = (formWidth - gap) / 2;
        int chooseWidth = Math.min(76, Math.max(58, half / 4));
        int selectorWidth = half - chooseWidth - gap;
        luckAttribute = field(x, 36, selectorWidth, luckAttributeValue, 256,
                value -> luckAttributeValue = value);
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.ATTRIBUTE,
                                this::chooseLuckAttribute))
                .bounds(x + selectorWidth + gap, 36, chooseWidth, 20).build());
        sampleLuck = field(x + half + gap, 36, formWidth - half - gap, sampleLuckValue, 32,
                value -> sampleLuckValue = value);
        luckFactors = field(x, 69, formWidth - chooseWidth - gap, luckFactorsValue, 4096,
                value -> luckFactorsValue = value);
        luckFactors.setHint(Component.translatable("screen.talent_editor.pool.luck_factors_hint"));
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.RARITY, this::chooseLuckFactor))
                .bounds(x + formWidth - chooseWidth, 69, chooseWidth, 20).build());
        pityRarity = field(x, 102, selectorWidth, pityRarityValue, 256,
                value -> pityRarityValue = value);
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.RARITY, this::choosePityRarity))
                .bounds(x + selectorWidth + gap, 102, chooseWidth, 20).build());
        pityAfter = field(x + half + gap, 102, formWidth - half - gap,
                pityAfterValue, 8, value -> pityAfterValue = value);
        policyButton = addRenderableWidget(EditorWidgets.button(policyLabel(), button -> choosePolicy())
                .bounds(x, 135, formWidth, 20).build());
        duplicateCounter = field(x, 168, selectorWidth, duplicateCounterValue, 256,
                value -> duplicateCounterValue = value);
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openPicker(ResourceReferencePickerScreen.Source.COUNTER,
                                this::chooseDuplicateCounter))
                .bounds(x + selectorWidth + gap, 168, chooseWidth, 20).build());
        duplicateAmount = field(x + half + gap, 168, formWidth - half - gap,
                duplicateAmountValue, 18, value -> duplicateAmountValue = value);
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), button -> save())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
    }

    private EditBox field(
            int x, int y, int width, String value, int maximumLength, Consumer<String> responder
    ) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maximumLength);
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
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int half = (formWidth - 6) / 2;
        EditorTheme.title(graphics,font,title,width);
        label(graphics, "screen.talent_editor.pool.luck_attribute", x, 25);
        label(graphics, "screen.talent_editor.pool.sample_luck", x + half + 6, 25);
        label(graphics, "screen.talent_editor.pool.luck_factors", x, 58);
        label(graphics, "screen.talent_editor.pool.pity_rarity", x, 91);
        label(graphics, "screen.talent_editor.pool.pity_after", x + half + 6, 91);
        label(graphics, "screen.talent_editor.pool.duplicate_counter", x, 157);
        label(graphics, "screen.talent_editor.pool.duplicate_amount", x + half + 6, 157);
        String preview = status.isBlank() ? probabilityPreview() : status;
        graphics.drawString(font, font.plainSubstrByWidth(preview, formWidth), x, 191,
                status.isBlank() ? 0x9FC5E8 : 0xFF7777, false);
        ReferenceDiagnostics.Summary references = referenceSummary();
        graphics.drawString(font, font.plainSubstrByWidth(
                ReferenceDiagnostics.label(references).getString(), formWidth
        ), x, 202, ReferenceDiagnostics.color(references), false);
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
        graphics.drawString(font, Component.translatable(key), x, y, EditorTheme.MUTED, false);
    }

    private void choosePolicy() {
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                POLICIES.stream().map(value -> Component.translatable(
                        "screen.talent_editor.pool.duplicate_policy." + value)).toList(),
                Math.max(0, POLICIES.indexOf(policy)), index -> {
                    policy = POLICIES.get(index);
                    policyButton.setMessage(policyLabel());
                    status = "";
                }, height);
    }

    private void openPicker(
            ResourceReferencePickerScreen.Source source,
            Consumer<ResourceLocation> selection
    ) {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(this, source, bootstrap, selection));
        }
    }

    private void chooseLuckAttribute(ResourceLocation id) {
        luckAttributeValue = id.toString();
    }

    private void chooseLuckFactor(ResourceLocation id) {
        luckFactorsValue = appendMapping(luckFactorsValue, id, "0");
    }

    private void choosePityRarity(ResourceLocation id) {
        pityRarityValue = id.toString();
    }

    private void chooseDuplicateCounter(ResourceLocation id) {
        duplicateCounterValue = id.toString();
    }

    private ReferenceDiagnostics.Summary referenceSummary() {
        return ReferenceDiagnostics.summarize(
                bootstrap, ResourceReferencePickerScreen.Source.ATTRIBUTE, luckAttributeValue, false
        ).plus(ReferenceDiagnostics.summarize(
                bootstrap, ResourceReferencePickerScreen.Source.RARITY, luckFactorsValue, true
        )).plus(ReferenceDiagnostics.summarize(
                bootstrap, ResourceReferencePickerScreen.Source.RARITY, pityRarityValue, false
        )).plus(ReferenceDiagnostics.summarize(
                bootstrap, ResourceReferencePickerScreen.Source.COUNTER, duplicateCounterValue, false
        ));
    }

    private Component policyLabel() {
        return Component.translatable("screen.talent_editor.pool.duplicate_policy",
                Component.translatable("screen.talent_editor.pool.duplicate_policy." + policy));
    }

    private void save() {
        Optional<ResourceLocation> luckId = parseOptionalId(luckAttribute.getValue());
        if (luckId == null) { invalid("screen.talent_editor.pool.invalid_luck_attribute"); return; }
        Map<ResourceLocation, Double> factors = parseMap(luckFactors.getValue(), -1_000.0D, 1_000.0D);
        if (factors == null) { invalid("screen.talent_editor.pool.invalid_luck_factors"); return; }
        if (luckId.isEmpty() && !factors.isEmpty()) {
            invalid("screen.talent_editor.pool.luck_attribute_required"); return;
        }
        Optional<ResourceLocation> pityId = parseOptionalId(pityRarity.getValue());
        if (pityId == null) { invalid("screen.talent_editor.pool.invalid_pity_rarity"); return; }
        Integer after = parseInt(pityAfter.getValue(), 0, 1_000_000);
        if (after == null || (after > 0 && pityId.isEmpty())) {
            invalid("screen.talent_editor.pool.invalid_pity_after"); return;
        }
        Optional<ResourceLocation> counterId = parseOptionalId(duplicateCounter.getValue());
        if (counterId == null) { invalid("screen.talent_editor.pool.invalid_duplicate_counter"); return; }
        Long amount = parseLong(duplicateAmount.getValue(), 1L, 1_000_000_000_000L);
        if (amount == null || (policy.equals("add_counter") && counterId.isEmpty())) {
            invalid("screen.talent_editor.pool.invalid_duplicate_amount"); return;
        }
        Optional<ResourceLocation> savedCounter = policy.equals("add_counter") ? counterId : Optional.empty();
        Optional<ResourceLocation> savedPity = after > 0 ? pityId : Optional.empty();
        parent.updateAdvanced(new PoolDraft.Advanced(
                luckId, factors, savedPity, after, policy, savedCounter, amount
        ));
        returnParent();
    }

    private String probabilityPreview() {
        Double luck = parseDouble(sampleLuck == null ? "0" : sampleLuck.getValue(), -1_024.0D, 1_024.0D);
        Map<ResourceLocation, Double> factors = luckFactors == null
                ? parent.advanced().luckFactors() : parseMap(luckFactors.getValue(), -1_000.0D, 1_000.0D);
        if (luck == null || factors == null) {
            return Component.translatable("screen.talent_editor.pool.probability_invalid").getString();
        }
        LinkedHashMap<String, Double> weights = new LinkedHashMap<>();
        for (EditorBootstrap.RaritySummary rarity : bootstrap.rarities()) {
            if (!rarity.enabled() || !rarity.offerable()) continue;
            double base = parent.rarityWeights().getOrDefault(rarity.id(), rarity.baseWeight());
            double multiplier = Math.max(0.0D, Math.min(1_000_000.0D,
                    1.0D + luck * factors.getOrDefault(rarity.id(), 0.0D)));
            double weight = base * multiplier;
            if (Double.isFinite(weight) && weight > 0.0D) weights.put(rarity.name(), weight);
        }
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!(total > 0.0D)) return Component.translatable("screen.talent_editor.pool.probability_empty").getString();
        String values = weights.entrySet().stream()
                .map(entry -> entry.getKey() + " " + String.format(Locale.ROOT, "%.2f%%", entry.getValue() / total * 100.0D))
                .reduce((left, right) -> left + " · " + right).orElse("-");
        return Component.translatable("screen.talent_editor.pool.probability_preview", values).getString();
    }

    private static Optional<ResourceLocation> parseOptionalId(String value) {
        String raw = value.strip();
        if (raw.isBlank()) return Optional.empty();
        ResourceLocation id = ResourceLocation.tryParse(raw);
        return id == null || !raw.contains(":") ? null : Optional.of(id);
    }

    private static Map<ResourceLocation, Double> parseMap(String value, double minimum, double maximum) {
        LinkedHashMap<ResourceLocation, Double> result = new LinkedHashMap<>();
        if (value.isBlank()) return result;
        for (String part : value.split(",")) {
            String entry = part.strip();
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) return null;
            String rawId = entry.substring(0, separator).strip();
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            Double number = parseDouble(entry.substring(separator + 1), minimum, maximum);
            if (id == null || !rawId.contains(":") || number == null || result.put(id, number) != null) return null;
        }
        return result;
    }

    private static Integer parseInt(String value, int minimum, int maximum) {
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed >= minimum && parsed <= maximum ? parsed : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    private static Long parseLong(String value, long minimum, long maximum) {
        try {
            long parsed = Long.parseLong(value.strip());
            return parsed >= minimum && parsed <= maximum ? parsed : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    private static Double parseDouble(String value, double minimum, double maximum) {
        try {
            double parsed = Double.parseDouble(value.strip());
            return Double.isFinite(parsed) && parsed >= minimum && parsed <= maximum ? parsed : null;
        } catch (NumberFormatException ignored) { return null; }
    }

    private void invalid(String key) {
        status = Component.translatable(key).getString();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }

    private static String formatMap(Map<ResourceLocation, Double> values) {
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + compact(entry.getValue()))
                .reduce((left, right) -> left + ", " + right).orElse("");
    }

    static String appendMapping(String value, ResourceLocation id, String defaultValue) {
        for (String raw : value.split(",")) {
            String entry = raw.strip();
            int equals = entry.indexOf('=');
            String key = equals < 0 ? entry : entry.substring(0, equals).strip();
            if (key.equals(id.toString())) return value;
        }
        String addition = id + "=" + defaultValue;
        return value.isBlank() ? addition : value.strip() + ", " + addition;
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.8f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }
}
