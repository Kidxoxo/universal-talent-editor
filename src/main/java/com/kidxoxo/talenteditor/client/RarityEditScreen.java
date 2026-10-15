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

public final class RarityEditScreen extends EditorDefinitionScreen {
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
    private final java.util.Map<Integer,String> fieldValues=new java.util.HashMap<>();
    private int fieldIndex;

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

    static RarityEditScreen copyOf(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new RarityEditScreen(bootstrap,RarityDraft.parse(json),id,false);
    }

    @Override
    protected void init() {
        fieldIndex=0;
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
        orderField.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.talent_editor.rarity.order")));
        weightField = field(x + (numberWidth + gap) * 2, 103,
                formWidth - (numberWidth + gap) * 2, compact(draft.baseWeight()), 32);

        int toggleWidth = (formWidth - gap) / 2;
        enabledButton = addRenderableWidget(EditorWidgets.button(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            ClientEditorState.clearValidation();
        }).bounds(x, 131, toggleWidth, 20).build());
        offerableButton = addRenderableWidget(EditorWidgets.button(offerableLabel(), button -> {
            offerable = !offerable;
            button.setMessage(offerableLabel());
            ClientEditorState.clearValidation();
        }).bounds(x + toggleWidth + gap, 131, formWidth - toggleWidth - gap, 20).build());

        int bottomY = height - 28;
        int bottomGap = 6;
        int bottomWidth = Math.max(54, (formWidth - bottomGap * 3) / 4);
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnList())
                .bounds(x, bottomY, bottomWidth, 20).build());
        int referenceCount = DefinitionReferenceScreen.inboundCount(
                bootstrap, DefinitionKind.RARITY, initialId
        );
        Button references = addRenderableWidget(EditorWidgets.button(
                        DefinitionReferenceScreen.buttonLabel(referenceCount), button -> definitionActions(bootstrap,ResourceLocation.tryParse(idField.getValue().strip()),existing,this::openReferences))
                .bounds(x + bottomWidth + bottomGap, bottomY, bottomWidth, 20).build());
        references.active = true;
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.validate"),
                        button -> submit(false))
                .bounds(x + (bottomWidth + bottomGap) * 2, bottomY, bottomWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.save"), EditorTheme.Tone.PRIMARY,
                        button -> submit(true))
                .bounds(x + (bottomWidth + bottomGap) * 3, bottomY,
                        formWidth - (bottomWidth + bottomGap) * 3, 20).build());
        watchDefinition(DefinitionKind.RARITY);
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        int key=fieldIndex++;
        field.setValue(fieldValues.getOrDefault(key,value));
        field.setResponder(next -> {fieldValues.put(key,next);ClientEditorState.clearValidation();});
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
        EditorTheme.title(graphics,font,title,width);
        label(graphics, "screen.talent_editor.rarity.id", x, 25);
        label(graphics, "screen.talent_editor.rarity.name", x, 58);
        label(graphics, "screen.talent_editor.rarity.color", x, 92);
        Integer preview = colorField == null ? null : RarityDraft.tryParseColor(colorField.getValue());
        graphics.fill(x + Math.min(65, numberWidth - 14), 91,
                x + Math.min(76, numberWidth - 3), 100,
                preview == null ? 0xFFFF5555 : 0xFF000000 | (preview & 0xFFFFFF));
        label(graphics, "screen.talent_editor.rarity.order_short", x + numberWidth + gap, 92);
        label(graphics, "screen.talent_editor.rarity.weight", x + (numberWidth + gap) * 2, 92);
        graphics.drawString(font, font.plainSubstrByWidth(
                Component.translatable("screen.talent_editor.rarity.help").getString(), formWidth
        ), x, 160, EditorTheme.MUTED, false);
        renderResult(graphics, x, formWidth);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, EditorTheme.MUTED, false);
    }

    private void renderResult(GuiGraphics graphics, int x, int formWidth) {
        renderSubmissionResult(graphics, x, formWidth, localStatus);
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
        if(submissionPending())return;
        RarityValues values = readValues();
        if (values == null) return;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        sendDefinition(baseRevision, values.id(), RarityDraft.parse(draft.snapshot()).toJson(
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
        EditorBootstrap bootstrap=latestBootstrap(this.bootstrap);
        if (!existing || minecraft == null) return;
        RarityValues values = readValues();
        if (values == null) return;
        RarityDraft.parse(draft.snapshot()).toJson(values.displayName(), enabled, RarityDraft.formatColor(values.color()),
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

    private void returnList() { leaveDefinition(this::leaveNow); }

    private void leaveNow() {
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

    @Override protected String draftState() { return fieldsState()+enabled+offerable+draft.snapshot(); }
    @Override protected void saveDraft() { submit(true); }
    @Override protected void committed(ValidationResultPacket value) { baseRevision=value.activeRevision(); existing=true; idField.setEditable(false); }
}
