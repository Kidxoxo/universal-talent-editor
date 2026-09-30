package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.ValidationResultPacket;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class CounterEditScreen extends Screen {
    private static final List<String> EVENTS = List.of(
            "manual", "kill", "damage_dealt", "damage_taken", "item_use", "entity_interact",
            "move_distance", "play_time"
    );
    private final EditorBootstrap bootstrap;
    private final CounterDraft draft;
    private final ResourceLocation initialId;
    private boolean existing;
    private long baseRevision;
    private boolean enabled;
    private String idValue;
    private String nameValue;
    private String eventValue;
    private String incrementValue;
    private String maximumValue;
    private String cooldownValue;
    private String intervalValue;
    private String requiredTalentsValue;
    private String requiredModsValue;
    private CounterDraft.Filters filters;
    private CounterDraft.AmountSettings amountSettings;
    private CounterDraft.ResetSettings resetSettings;
    private CounterDraft.MetadataSettings metadataSettings;
    private EditBox idField;
    private EditBox nameField;
    private Button eventButton;
    private String localStatus = "";
    private final EditorOptionPopup options = new EditorOptionPopup();

    private CounterEditScreen(EditorBootstrap bootstrap, CounterDraft draft, ResourceLocation id, boolean existing) {
        super(Component.translatable(existing
                ? "screen.talent_editor.counter.edit.title" : "screen.talent_editor.counter.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.initialId = id;
        this.existing = existing;
        this.baseRevision = bootstrap.revision();
        this.enabled = draft.enabled();
        this.idValue = id.toString();
        this.nameValue = draft.name();
        this.eventValue = EVENTS.contains(draft.event()) ? draft.event() : "kill";
        this.incrementValue = Long.toString(draft.increment());
        this.maximumValue = Long.toString(draft.maximum());
        this.cooldownValue = Integer.toString(draft.cooldownTicks());
        this.intervalValue = Integer.toString(draft.intervalTicks());
        this.requiredTalentsValue = joinIds(draft.requiredTalents());
        this.requiredModsValue = String.join(", ", draft.requiredMods());
        this.filters = draft.filters();
        this.amountSettings = draft.amountSettings();
        this.resetSettings = draft.resetSettings();
        this.metadataSettings = draft.metadataSettings();
    }

    public static CounterEditScreen createNew(EditorBootstrap bootstrap) {
        return new CounterEditScreen(
                bootstrap, CounterDraft.create(), new ResourceLocation("custom", "new_counter"), false
        );
    }

    public static CounterEditScreen editExisting(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new CounterEditScreen(bootstrap, CounterDraft.parse(json), id, true);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        idField = field(x, 32, formWidth, idValue, 256, value -> idValue = value);
        idField.setEditable(!existing);
        nameField = field(x, 59, formWidth, nameValue, 512, value -> nameValue = value);
        int half = (formWidth - 6) / 2;
        eventButton = addRenderableWidget(Button.builder(eventLabel(), button -> chooseEvent())
                .bounds(x, 86, half, 20).build());
        addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            ClientEditorState.clearValidation();
        }).bounds(x + half + 6, 86, formWidth - half - 6, 20).build());

        int gap = 5;
        int part = (formWidth - gap * 3) / 4;
        field(x, 119, part, incrementValue, 18, value -> incrementValue = value);
        field(x + part + gap, 119, part, maximumValue, 18, value -> maximumValue = value);
        field(x + (part + gap) * 2, 119, part, cooldownValue, 10, value -> cooldownValue = value);
        field(x + (part + gap) * 3, 119, formWidth - (part + gap) * 3,
                intervalValue, 10, value -> intervalValue = value);
        field(x, 151, formWidth, requiredTalentsValue, 4096, value -> requiredTalentsValue = value);
        field(x, 183, formWidth, requiredModsValue, 2048, value -> requiredModsValue = value);

        int bottom = height - 28;
        int buttonGap = 5;
        int buttonWidth = Math.max(36, (formWidth - buttonGap * 7) / 8);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnList())
                .bounds(x, bottom, buttonWidth, 20).build());
        int referenceCount = DefinitionReferenceScreen.inboundCount(
                bootstrap, DefinitionKind.COUNTER, initialId
        );
        Button references = addRenderableWidget(Button.builder(
                        DefinitionReferenceScreen.buttonLabel(referenceCount), button -> openReferences())
                .bounds(x + buttonWidth + buttonGap, bottom, buttonWidth, 20).build());
        references.active = existing;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.counter.filters"),
                        button -> openFilters())
                .bounds(x + (buttonWidth + buttonGap) * 2, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.counter.amount"),
                        button -> openAmount())
                .bounds(x + (buttonWidth + buttonGap) * 3, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.validate"),
                        button -> submit(false))
                .bounds(x + (buttonWidth + buttonGap) * 6, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.save"),
                        button -> submit(true))
                .bounds(x + (buttonWidth + buttonGap) * 7, bottom,
                        formWidth - (buttonWidth + buttonGap) * 7, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.counter.reset"),
                        button -> openReset())
                .bounds(x + (buttonWidth + buttonGap) * 4, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.counter.metadata"),
                        button -> openMetadata())
                .bounds(x + (buttonWidth + buttonGap) * 5, bottom, buttonWidth, 20).build());
    }

    private EditBox field(int x, int y, int width, String value, int maxLength,
                          java.util.function.Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
            ClientEditorState.clearValidation();
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
        int gap = 5;
        int part = (formWidth - gap * 3) / 4;
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        label(graphics, "screen.talent_editor.counter.id", x, 21);
        label(graphics, "screen.talent_editor.counter.name", x, 48);
        label(graphics, "screen.talent_editor.counter.increment", x, 108);
        label(graphics, "screen.talent_editor.counter.maximum", x + part + gap, 108);
        label(graphics, "screen.talent_editor.counter.cooldown", x + (part + gap) * 2, 108);
        label(graphics, "screen.talent_editor.counter.interval", x + (part + gap) * 3, 108);
        label(graphics, "screen.talent_editor.counter.required_talents", x, 140);
        label(graphics, "screen.talent_editor.counter.required_mods", x, 172);
        if (height >= 280) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.counter.amount_summary",
                            Component.translatable("screen.talent_editor.counter.amount_mode." + amountSettings.mode()),
                            compact(amountSettings.scale()),
                            Component.translatable("screen.talent_editor.counter.rounding." + amountSettings.rounding()),
                            amountSettings.minimumIncrement()),
                    x, 215, 0xA0A0A0, false);
            graphics.drawString(font, Component.translatable("screen.talent_editor.counter.reset_summary",
                            yesNo(resetSettings.onLogin()), yesNo(resetSettings.onDeath()),
                            resetSettings.intervalTicks()),
                    x, 226, 0xA0A0A0, false);
        }
        renderResult(graphics, x, formWidth);
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
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    private void renderResult(GuiGraphics graphics, int x, int formWidth) {
        ValidationResultPacket result = ClientEditorState.validation();
        String message = localStatus;
        int color = 0xA0A0A0;
        if (result != null) {
            if (result.committed()) {
                message = Component.translatable("screen.talent_editor.result.saved", result.activeRevision()).getString();
                color = 0x55FF55;
                if (result.activeRevision() > baseRevision) {
                    baseRevision = result.activeRevision();
                    existing = true;
                    idField.setEditable(false);
                }
            } else if (result.accepted()) {
                message = Component.translatable("screen.talent_editor.result.valid").getString();
                color = 0x55FF55;
            } else if (result.conflict()) {
                message = Component.translatable("screen.talent_editor.result.conflict", result.activeRevision()).getString();
                color = 0xFF5555;
            } else {
                ValidationIssue first = result.issues().isEmpty() ? null : result.issues().get(0);
                message = first == null ? Component.translatable("screen.talent_editor.result.invalid").getString()
                        : first.code() + " " + first.path() + ": " + first.message();
                color = 0xFF7777;
            }
        }
        if (!message.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(message, formWidth),
                x, height - 42, color, false);
    }

    private void chooseEvent() {
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                EVENTS.stream().map(value -> Component.translatable(
                        "screen.talent_editor.counter.event." + value)).toList(),
                Math.max(0, EVENTS.indexOf(eventValue)), index -> {
                    eventValue = EVENTS.get(index);
                    eventButton.setMessage(eventLabel());
                    ClientEditorState.clearValidation();
                }, height);
    }

    private Component eventLabel() {
        return Component.translatable("screen.talent_editor.counter.event", Component.translatable(
                "screen.talent_editor.counter.event." + eventValue
        ));
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.counter.enabled_yes" : "screen.talent_editor.counter.enabled_no");
    }

    private void submit(boolean commit) {
        ResourceLocation id = ResourceLocation.tryParse(idValue.strip());
        if (id == null) { invalid("screen.talent_editor.counter.invalid_id"); return; }
        if (nameValue.strip().isBlank()) { invalid("screen.talent_editor.form.empty_name"); return; }
        long increment = parseLong(incrementValue, 1L, 1_000_000_000_000L);
        long maximum = parseLong(maximumValue, 1L, 1_000_000_000_000L);
        int cooldown = parseInt(cooldownValue, 0, 1_200_000);
        int interval = parseInt(intervalValue, 1, 1_200_000);
        if (increment < 0L || maximum < 0L || cooldown < 0 || interval < 0) {
            invalid("screen.talent_editor.counter.invalid_numbers"); return;
        }
        List<ResourceLocation> requiredTalents = parseIds(requiredTalentsValue);
        if (requiredTalents == null) { invalid("screen.talent_editor.counter.invalid_talents"); return; }
        List<String> requiredMods = parseMods(requiredModsValue);
        if (requiredMods == null) { invalid("screen.talent_editor.counter.invalid_mods"); return; }
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        EditorNetwork.submitCounter(baseRevision, id, draft.toJson(
                nameValue.strip(), enabled, eventValue, increment, maximum, cooldown, interval,
                resetSettings, metadataSettings, requiredMods, requiredTalents, filters
        ), commit);
    }

    private void invalid(String key) {
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(key).getString();
    }

    CounterDraft.Filters filters() {
        return filters;
    }

    EditorBootstrap bootstrap() {
        return bootstrap;
    }

    void updateFilters(CounterDraft.Filters filters) {
        this.filters = filters;
        ClientEditorState.clearValidation();
    }

    CounterDraft.AmountSettings amountSettings() {
        return amountSettings;
    }

    void updateAmountSettings(CounterDraft.AmountSettings amountSettings) {
        this.amountSettings = amountSettings;
        draft.updateAmountSettings(amountSettings);
        ClientEditorState.clearValidation();
    }

    CounterDraft.ResetSettings resetSettings() {
        return resetSettings;
    }

    void updateResetSettings(CounterDraft.ResetSettings resetSettings) {
        this.resetSettings = resetSettings;
        ClientEditorState.clearValidation();
    }

    CounterDraft.MetadataSettings metadataSettings() {
        return metadataSettings;
    }

    void updateMetadataSettings(CounterDraft.MetadataSettings metadataSettings) {
        this.metadataSettings = metadataSettings;
        ClientEditorState.clearValidation();
    }

    private void openFilters() {
        if (minecraft != null) minecraft.setScreen(new CounterFilterScreen(this));
    }

    private void openAmount() {
        if (minecraft != null) minecraft.setScreen(new CounterAmountScreen(this));
    }

    private void openReset() {
        if (minecraft != null) minecraft.setScreen(new CounterResetScreen(this));
    }

    private void openMetadata() {
        if (minecraft != null) minecraft.setScreen(new CounterMetadataScreen(this));
    }

    private void openReferences() {
        if (!existing || minecraft == null) return;
        int count = DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.COUNTER, initialId);
        minecraft.setScreen(new DefinitionReferenceScreen(
                this, bootstrap, DefinitionKind.COUNTER, initialId, count
        ));
    }

    private void returnList() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new CounterListScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnList();
    }

    static List<ResourceLocation> parseIds(String value) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        if (value.isBlank()) return List.of();
        for (String part : value.split(",")) {
            String raw = part.strip();
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (raw.isBlank() || id == null || !raw.contains(":") || !ids.add(id)) return null;
        }
        return List.copyOf(ids);
    }

    static String joinIds(List<ResourceLocation> values) {
        return String.join(", ", values.stream().map(ResourceLocation::toString).toList());
    }

    private static List<String> parseMods(String value) {
        ArrayList<String> mods = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        if (value.isBlank()) return List.of();
        for (String part : value.split(",")) {
            String mod = part.strip().toLowerCase(Locale.ROOT);
            if (!mod.matches("[a-z][a-z0-9_-]{1,63}") || !seen.add(mod)) return null;
            mods.add(mod);
        }
        return List.copyOf(mods);
    }

    private static long parseLong(String value, long min, long max) {
        try {
            long parsed = Long.parseLong(value.strip());
            return parsed >= min && parsed <= max ? parsed : -1L;
        } catch (NumberFormatException ignored) { return -1L; }
    }

    private static int parseInt(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed >= min && parsed <= max ? parsed : -1;
        } catch (NumberFormatException ignored) { return -1; }
    }


    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.6f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }

    private static Component yesNo(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }
}
