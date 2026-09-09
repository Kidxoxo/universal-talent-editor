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

import java.util.Locale;

public final class RarityEditScreen extends Screen {
    private final EditorBootstrap bootstrap;
    private final RarityDraft draft;
    private final ResourceLocation initialId;
    private boolean existing;
    private long baseRevision;
    private boolean enabled;
    private boolean offerable;
    private EditBox idField;
    private EditBox nameField;
    private EditBox colorField;
    private EditBox orderField;
    private EditBox weightField;
    private Button enabledButton;
    private Button offerableButton;
    private String localStatus = "";

    private RarityEditScreen(
            EditorBootstrap bootstrap,
            RarityDraft draft,
            ResourceLocation id,
            boolean existing
    ) {
        super(Component.translatable(existing
                ? "screen.talent_editor.rarity.edit.title"
                : "screen.talent_editor.rarity.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.initialId = id;
        this.existing = existing;
        this.baseRevision = bootstrap.revision();
        this.enabled = draft.enabled();
        this.offerable = draft.offerable();
    }

    public static RarityEditScreen createNew(EditorBootstrap bootstrap) {
        return new RarityEditScreen(
                bootstrap, RarityDraft.create(), new ResourceLocation("custom", "new_rarity"), false
        );
    }

    public static RarityEditScreen editExisting(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new RarityEditScreen(bootstrap, RarityDraft.parse(json), id, true);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        idField = field(x, 36, formWidth, initialId.toString(), 256);
        idField.setEditable(!existing);
        nameField = field(x, 69, formWidth, draft.name(), 512);

        int gap = 6;
        int numberWidth = (formWidth - gap * 2) / 3;
        colorField = field(x, 103, numberWidth, draft.color(), 9);
        orderField = field(x + numberWidth + gap, 103, numberWidth,
                Integer.toString(draft.order()), 8);
        weightField = field(x + (numberWidth + gap) * 2, 103,
                formWidth - (numberWidth + gap) * 2, compact(draft.baseWeight()), 32);

        int toggleWidth = (formWidth - gap) / 2;
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            ClientEditorState.clearValidation();
        }).bounds(x, 131, toggleWidth, 20).build());
        offerableButton = addRenderableWidget(Button.builder(offerableLabel(), button -> {
            offerable = !offerable;
            button.setMessage(offerableLabel());
            ClientEditorState.clearValidation();
        }).bounds(x + toggleWidth + gap, 131, formWidth - toggleWidth - gap, 20).build());

        int bottomY = height - 28;
        int bottomGap = 6;
        int bottomWidth = Math.max(54, (formWidth - bottomGap * 3) / 4);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnList())
                .bounds(x, bottomY, bottomWidth, 20).build());
        int referenceCount = DefinitionReferenceScreen.inboundCount(
                bootstrap, DefinitionKind.RARITY, initialId
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
        int numberWidth = (formWidth - gap * 2) / 3;
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        label(graphics, "screen.talent_editor.rarity.id", x, 25);
        label(graphics, "screen.talent_editor.rarity.name", x, 58);
        label(graphics, "screen.talent_editor.rarity.color", x, 92);
        Integer preview = colorField == null ? null : RarityDraft.tryParseColor(colorField.getValue());
        graphics.fill(x + Math.min(65, numberWidth - 14), 91,
                x + Math.min(76, numberWidth - 3), 100,
                preview == null ? 0xFFFF5555 : 0xFF000000 | (preview & 0xFFFFFF));
        label(graphics, "screen.talent_editor.rarity.order", x + numberWidth + gap, 92);
        label(graphics, "screen.talent_editor.rarity.weight", x + (numberWidth + gap) * 2, 92);
        graphics.drawString(font, font.plainSubstrByWidth(
                Component.translatable("screen.talent_editor.rarity.help").getString(), formWidth
        ), x, 160, 0x808080, false);
        renderResult(graphics, x, formWidth);
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
                ? "screen.talent_editor.rarity.enabled_yes"
                : "screen.talent_editor.rarity.enabled_no");
    }

    private Component offerableLabel() {
        return Component.translatable(offerable
                ? "screen.talent_editor.rarity.offerable_yes"
                : "screen.talent_editor.rarity.offerable_no");
    }

    private void submit(boolean commit) {
        RarityValues values = readValues();
        if (values == null) return;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        EditorNetwork.submitRarity(baseRevision, values.id(), draft.toJson(
                values.displayName(), enabled, RarityDraft.formatColor(values.color()),
                values.order(), offerable, values.weight()
        ), commit);
    }

    private RarityValues readValues() {
        ResourceLocation id = ResourceLocation.tryParse(idField.getValue().strip());
        if (id == null) {
            invalid("screen.talent_editor.rarity.invalid_id");
            return null;
        }
        String displayName = nameField.getValue().strip();
        if (displayName.isBlank()) {
            invalid("screen.talent_editor.form.empty_name");
            return null;
        }
        Integer color = RarityDraft.tryParseColor(colorField.getValue());
        if (color == null) {
            invalid("screen.talent_editor.rarity.invalid_color");
            return null;
        }
        int order;
        try {
            order = Integer.parseInt(orderField.getValue().strip());
            if (order < -100_000 || order > 100_000) throw new NumberFormatException();
        } catch (NumberFormatException ignored) {
            invalid("screen.talent_editor.rarity.invalid_order");
            return null;
        }
        double weight;
        try {
            weight = Double.parseDouble(weightField.getValue().strip());
            if (!Double.isFinite(weight) || weight < 0.0D || weight > 1_000_000_000.0D) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ignored) {
            invalid("screen.talent_editor.rarity.invalid_weight");
            return null;
        }
        return new RarityValues(id, displayName, color, order, weight);
    }

    private void openReferences() {
        if (!existing || minecraft == null) return;
        RarityValues values = readValues();
        if (values == null) return;
        draft.toJson(values.displayName(), enabled, RarityDraft.formatColor(values.color()),
                values.order(), offerable, values.weight());
        int count = DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.RARITY, values.id());
        minecraft.setScreen(new DefinitionReferenceScreen(
                this, bootstrap, DefinitionKind.RARITY, values.id(), count
        ));
    }

    private void invalid(String key) {
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(key).getString();
    }

    private void returnList() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new RarityListScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnList();
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.8f", value)
                .replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }

    private record RarityValues(
            ResourceLocation id, String displayName, int color, int order, double weight
    ) {
    }
}
