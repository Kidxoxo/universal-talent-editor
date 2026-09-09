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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class PoolEditScreen extends Screen {
    private final EditorBootstrap bootstrap;
    private final PoolDraft draft;
    private boolean existing;
    private long baseRevision;
    private String idValue;

    private EditBox idField;
    private EditBox nameField;
    private EditBox choicesField;
    private EditBox refreshesField;
    private EditBox maxOwnedField;
    private EditBox iconSizeField;
    private EditBox weightsField;
    private Button enabledButton;
    private boolean enabled;
    private String localStatus = "";

    private PoolEditScreen(
            EditorBootstrap bootstrap,
            PoolDraft draft,
            ResourceLocation id,
            boolean existing
    ) {
        super(Component.translatable(existing
                ? "screen.talent_editor.pool.edit.title"
                : "screen.talent_editor.pool.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.idValue = id.toString();
        this.existing = existing;
        this.baseRevision = bootstrap.revision();
        this.enabled = draft.enabled();
    }

    public static PoolEditScreen createNew(EditorBootstrap bootstrap) {
        return new PoolEditScreen(
                bootstrap, PoolDraft.create(), new ResourceLocation("custom", "new_pool"), false
        );
    }

    public static PoolEditScreen editExisting(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new PoolEditScreen(bootstrap, PoolDraft.parse(json), id, true);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        idField = field(x, 36, formWidth, idValue, 256);
        idField.setResponder(value -> {
            idValue = value;
            ClientEditorState.clearValidation();
        });
        idField.setEditable(!existing);
        nameField = field(x, 69, formWidth, draft.name(), 512);

        int gap = 6;
        int numberWidth = (formWidth - gap * 3) / 4;
        choicesField = field(x, 103, numberWidth, Integer.toString(draft.choices()), 5);
        refreshesField = field(x + numberWidth + gap, 103, numberWidth,
                Integer.toString(draft.refreshes()), 5);
        maxOwnedField = field(x + (numberWidth + gap) * 2, 103,
                numberWidth, Integer.toString(draft.maxOwned()), 6);
        iconSizeField = field(x + (numberWidth + gap) * 3, 103,
                formWidth - (numberWidth + gap) * 3, Integer.toString(draft.iconSize()), 2);

        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            ClientEditorState.clearValidation();
        }).bounds(x, 131, (formWidth - gap) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.pool.advanced"),
                        button -> openAdvanced())
                .bounds(x + (formWidth - gap) / 2 + gap, 131,
                        formWidth - (formWidth - gap) / 2 - gap, 20).build());

        weightsField = field(x, 175, formWidth, formatWeights(draft.rarityWeights()), 4096);
        weightsField.setHint(Component.translatable("screen.talent_editor.pool.weights_hint"));

        int bottomY = height - 28;
        int bottomGap = 6;
        int bottomWidth = Math.max(54, (formWidth - bottomGap * 3) / 4);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnList())
                .bounds(x, bottomY, bottomWidth, 20).build());
        int referenceCount = DefinitionReferenceScreen.inboundCount(
                bootstrap, DefinitionKind.POOL, ResourceLocation.tryParse(idValue)
        );
        Button references = addRenderableWidget(Button.builder(
                        DefinitionReferenceScreen.buttonLabel(referenceCount), button -> openReferences())
                .bounds(x + bottomWidth + bottomGap, bottomY, bottomWidth, 20).build());
        references.active = existing;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.validate"),
                        button -> submit(false))
                .bounds(x + (bottomWidth + bottomGap) * 2, bottomY, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.save"),
                        button -> submit(true))
                .bounds(x + (bottomWidth + bottomGap) * 3, bottomY,
                        formWidth - (bottomWidth + bottomGap) * 3, 20).build());
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(ignored -> ClientEditorState.clearValidation());
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int numberWidth = (formWidth - gap * 3) / 4;
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        label(graphics, "screen.talent_editor.pool.id", x, 25);
        label(graphics, "screen.talent_editor.pool.name", x, 58);
        label(graphics, "screen.talent_editor.pool.choices", x, 92, numberWidth);
        label(graphics, "screen.talent_editor.pool.refreshes", x + numberWidth + gap, 92, numberWidth);
        label(graphics, "screen.talent_editor.pool.max_owned", x + (numberWidth + gap) * 2, 92, numberWidth);
        label(graphics, "screen.talent_editor.pool.icon_size", x + (numberWidth + gap) * 3, 92,
                formWidth - (numberWidth + gap) * 3);
        label(graphics, "screen.talent_editor.pool.weights", x, 164);
        graphics.drawString(font, font.plainSubstrByWidth(
                Component.translatable("screen.talent_editor.pool.weights_help").getString(), formWidth
        ), x, 198, 0x808080, false);
        renderResult(graphics, x, formWidth);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    private void label(GuiGraphics graphics, String key, int x, int y, int maximumWidth) {
        String text = Component.translatable(key).getString();
        graphics.drawString(font, font.plainSubstrByWidth(text, Math.max(8, maximumWidth)),
                x, y, 0xA0A0A0, false);
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
                message = Component.translatable("screen.talent_editor.result.conflict",
                        result.activeRevision()).getString();
                color = 0xFF5555;
            } else {
                ValidationIssue first = result.issues().isEmpty() ? null : result.issues().get(0);
                message = first == null
                        ? Component.translatable("screen.talent_editor.result.invalid").getString()
                        : first.code() + " " + first.path() + ": " + first.message();
                color = 0xFF7777;
            }
        }
        if (!message.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(message, formWidth),
                    x, height - 47, color, false);
        }
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.pool.enabled_yes"
                : "screen.talent_editor.pool.enabled_no");
    }

    private void submit(boolean commit) {
        BaseValues values = readBase();
        if (values == null) return;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        EditorNetwork.submitPool(baseRevision, values.id(), draft.toJson(
                values.displayName(), enabled, values.choices(), values.refreshes(), values.maxOwned(),
                values.iconSize(), values.weights(), draft.advanced()
        ), commit);
    }

    private BaseValues readBase() {
        ResourceLocation id = ResourceLocation.tryParse(idField.getValue().strip());
        if (id == null) {
            invalid("screen.talent_editor.pool.invalid_id");
            return null;
        }
        String displayName = nameField.getValue().strip();
        if (displayName.isBlank()) {
            invalid("screen.talent_editor.form.empty_name");
            return null;
        }
        Integer choices = parseInteger(choicesField, 1, 5);
        if (choices == null) {
            invalid("screen.talent_editor.pool.invalid_choices");
            return null;
        }
        Integer refreshes = parseInteger(refreshesField, 0, 100);
        if (refreshes == null) {
            invalid("screen.talent_editor.pool.invalid_refreshes");
            return null;
        }
        Integer maxOwned = parseInteger(maxOwnedField, 1, 10_000);
        if (maxOwned == null) {
            invalid("screen.talent_editor.pool.invalid_max_owned");
            return null;
        }
        Integer iconSize = parseInteger(iconSizeField, 8, 64);
        if (iconSize == null) {
            invalid("screen.talent_editor.pool.invalid_icon_size");
            return null;
        }
        Map<ResourceLocation, Double> weights = parseWeights();
        if (weights == null) return null;
        return new BaseValues(id, displayName, choices, refreshes, maxOwned, iconSize, weights);
    }

    private void openAdvanced() {
        BaseValues values = readBase();
        if (values == null || minecraft == null) return;
        draft.toJson(values.displayName(), enabled, values.choices(), values.refreshes(), values.maxOwned(),
                values.iconSize(), values.weights(), draft.advanced());
        minecraft.setScreen(new PoolAdvancedScreen(this, bootstrap));
    }

    private void openReferences() {
        if (!existing || minecraft == null) return;
        BaseValues values = readBase();
        if (values == null) return;
        draft.toJson(values.displayName(), enabled, values.choices(), values.refreshes(), values.maxOwned(),
                values.iconSize(), values.weights(), draft.advanced());
        int count = DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.POOL, values.id());
        minecraft.setScreen(new DefinitionReferenceScreen(
                this, bootstrap, DefinitionKind.POOL, values.id(), count
        ));
    }

    PoolDraft.Advanced advanced() {
        return draft.advanced();
    }

    Map<ResourceLocation, Double> rarityWeights() {
        return draft.rarityWeights();
    }

    void updateAdvanced(PoolDraft.Advanced advanced) {
        draft.updateAdvanced(advanced);
        ClientEditorState.clearValidation();
    }

    private Map<ResourceLocation, Double> parseWeights() {
        LinkedHashMap<ResourceLocation, Double> result = new LinkedHashMap<>();
        String value = weightsField.getValue().strip();
        if (value.isBlank()) return result;
        for (String rawEntry : value.split(",")) {
            String entry = rawEntry.strip();
            if (entry.isBlank()) continue;
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                invalid("screen.talent_editor.pool.invalid_weight_entry", entry);
                return null;
            }
            ResourceLocation rarity = ResourceLocation.tryParse(entry.substring(0, separator).strip());
            double weight;
            try {
                weight = Double.parseDouble(entry.substring(separator + 1).strip());
            } catch (NumberFormatException ignored) {
                invalid("screen.talent_editor.pool.invalid_weight_entry", entry);
                return null;
            }
            if (rarity == null || !Double.isFinite(weight) || weight < 0.0D || weight > 1_000_000_000.0D) {
                invalid("screen.talent_editor.pool.invalid_weight_entry", entry);
                return null;
            }
            result.put(rarity, weight);
        }
        return result;
    }

    private void invalid(String key, Object... arguments) {
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(key, arguments).getString();
    }

    private static Integer parseInteger(EditBox field, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(field.getValue().strip());
            return value >= minimum && value <= maximum ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void returnList() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new PoolListScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnList();
    }

    private static String formatWeights(Map<ResourceLocation, Double> weights) {
        return weights.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + compact(entry.getValue()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.8f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }

    private record BaseValues(
            ResourceLocation id,
            String displayName,
            int choices,
            int refreshes,
            int maxOwned,
            int iconSize,
            Map<ResourceLocation, Double> weights
    ) {
    }
}
