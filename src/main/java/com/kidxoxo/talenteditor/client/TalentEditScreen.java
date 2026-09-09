package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.ValidationResultPacket;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TalentEditScreen extends Screen {
    private static final int ROW_HEIGHT = 18;

    private final EditorBootstrap bootstrap;
    private final TalentDraft draft;
    private boolean existing;
    private long baseRevision;
    private String idValue;
    private String nameValue;
    private EditBox idField;
    private EditBox nameField;
    private Button editEffectButton;
    private Button removeButton;
    private int selectedRawIndex = -1;
    private int effectScroll;
    private String localStatus = "";

    private TalentEditScreen(
            EditorBootstrap bootstrap,
            TalentDraft draft,
            ResourceLocation id,
            boolean existing
    ) {
        super(Component.translatable(existing
                ? "screen.talent_editor.edit.title"
                : "screen.talent_editor.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.existing = existing;
        this.baseRevision = bootstrap.revision();
        this.idValue = id.toString();
        this.nameValue = draft.name();
    }

    public static TalentEditScreen createNew(EditorBootstrap bootstrap) {
        return new TalentEditScreen(
                bootstrap, TalentDraft.create(), new ResourceLocation("custom", "new_talent"), false
        );
    }

    public static TalentEditScreen editExisting(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new TalentEditScreen(bootstrap, TalentDraft.parse(json), id, true);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        idField = new EditBox(font, x, 43, formWidth, 20,
                Component.translatable("screen.talent_editor.form.id"));
        idField.setMaxLength(256);
        idField.setValue(idValue);
        idField.setEditable(!existing);
        addRenderableWidget(idField);

        int settingsWidth = Math.min(150, Math.max(104, formWidth / 3));
        nameField = new EditBox(font, x, 76, Math.max(56, formWidth - settingsWidth - 6), 20,
                Component.translatable("screen.talent_editor.form.name"));
        nameField.setMaxLength(512);
        nameField.setValue(nameValue);
        addRenderableWidget(nameField);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.settings_menu.open"),
                        button -> openSettingsMenu())
                .bounds(x + formWidth - settingsWidth, 76, settingsWidth, 20).build());

        int actionY = height - 68;
        int actionWidth = (formWidth - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.add_effect"),
                        button -> openEffectEditor(-1))
                .bounds(x, actionY, actionWidth, 20).build());
        editEffectButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.edit_selected_effect"),
                        button -> editSelectedEffect())
                .bounds(x + actionWidth + 6, actionY, actionWidth, 20).build());
        removeButton = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.remove_effect"),
                        button -> removeSelected())
                .bounds(x + (actionWidth + 6) * 2, actionY,
                        formWidth - (actionWidth + 6) * 2, 20)
                .build());

        int bottomY = height - 28;
        int bottomGap = 6;
        int bottomWidth = Math.max(54, (formWidth - bottomGap * 3) / 4);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnHome())
                .bounds(x, bottomY, bottomWidth, 20).build());
        int referenceCount = DefinitionReferenceScreen.inboundCount(
                bootstrap, DefinitionKind.TALENT, ResourceLocation.tryParse(idValue)
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
        syncSelectionWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, 75);
        EditorTheme.panel(graphics, x - 7, 108, formWidth + 14, Math.max(1, height - 183));
        EditorTheme.panel(graphics, x - 7, height - 75, formWidth + 14, 39);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawString(font, Component.translatable("screen.talent_editor.form.id"), x, 32,
                EditorTheme.MUTED, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.form.name"), x, 65,
                EditorTheme.MUTED, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.effects", draft.allEffectRows().size()),
                x, 103, EditorTheme.TEXT, false);
        renderEffects(graphics, mouseX, mouseY, x, formWidth);
        renderResult(graphics, x, formWidth);
    }

    private void renderEffects(GuiGraphics graphics, int mouseX, int mouseY, int x, int formWidth) {
        List<TalentDraft.EffectRow> rows = draft.allEffectRows();
        int top = 119;
        int bottom = height - 76;
        int visibleRows = Math.max(1, (bottom - top) / ROW_HEIGHT);
        effectScroll = Math.max(0, Math.min(effectScroll, Math.max(0, rows.size() - visibleRows)));
        int end = Math.min(rows.size(), effectScroll + visibleRows);
        for (int index = effectScroll; index < end; index++) {
            TalentDraft.EffectRow row = rows.get(index);
            int y = top + (index - effectScroll) * ROW_HEIGHT;
            boolean selected = row.rawIndex() == selectedRawIndex;
            boolean hovered = mouseX >= x && mouseX < x + formWidth && mouseY >= y - 3 && mouseY < y + 14;
            if (selected || hovered) {
                graphics.fill(x, y - 3, x + formWidth, y + 14,
                        selected ? 0x7050A0E8 : 0x404A90E2);
            }
            String label = effectLabel(row);
            graphics.drawString(font, font.plainSubstrByWidth(label, formWidth - 8), x + 4, y,
                    row.editableAttribute() || row.editableImmunity() || row.editableMaintainedEffect()
                            || row.editableDrawRefreshBonus() || row.editableDynamicAttribute()
                            || row.editableCreativeFlight() || draft.isEditableEventEffect(row)
                            ? 0xE8E8E8 : 0x909090, false);
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.no_effects"),
                    width / 2, top + 8, 0x808080);
        }
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
            graphics.drawString(font, font.plainSubstrByWidth(message, formWidth), x, height - 47, color, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int formWidth = Math.min(650, width - 32);
            int x = (width - formWidth) / 2;
            int top = 119;
            int bottom = height - 76;
            if (mouseX >= x && mouseX < x + formWidth && mouseY >= top && mouseY < bottom) {
                int rowIndex = effectScroll + (int) ((mouseY - top) / ROW_HEIGHT);
                List<TalentDraft.EffectRow> rows = draft.allEffectRows();
                if (rowIndex >= 0 && rowIndex < rows.size()) {
                    select(rows.get(rowIndex));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rows = Math.max(1, (height - 76 - 119) / ROW_HEIGHT);
        effectScroll = Math.max(0, Math.min(
                effectScroll - (int) Math.signum(delta) * 3,
                Math.max(0, draft.allEffectRows().size() - rows)
        ));
        return true;
    }

    public void addAttribute(ResourceLocation attribute) {
        addAttributeEffect(attribute);
    }

    int addAttributeEffect(ResourceLocation attribute) {
        captureHeaderFields();
        selectedRawIndex = draft.addAttribute(attribute);
        syncSelectionWidgets();
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.attribute_added", attribute.toString()).getString();
        return selectedRawIndex;
    }

    int addEffectImmunity(ResourceLocation effect) {
        captureHeaderFields();
        selectedRawIndex = draft.addEffectImmunity(effect);
        syncSelectionWidgets();
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.immunity_added", effect.toString()).getString();
        return selectedRawIndex;
    }

    int addMaintainedEffect(ResourceLocation effect) {
        captureHeaderFields();
        selectedRawIndex = draft.addMaintainedEffect(effect);
        syncSelectionWidgets();
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.maintained_effect_added",
                effect.toString()).getString();
        return selectedRawIndex;
    }

    public TalentDraft.OfferDraft offer() {
        return draft.offer();
    }

    public TalentDraft.MetadataDraft metadata() {
        return draft.metadata();
    }

    public TalentDraft.TextStylesDraft textStyles() {
        return draft.textStyles();
    }

    public void updateTextStyles(LocalizedTextDraft name, List<LocalizedTextDraft> descriptions) {
        draft.updateTextStyles(name, descriptions);
        nameValue = name.fallbackText();
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.text_updated").getString();
    }

    public List<EditorBootstrap.RaritySummary> rarities() {
        return bootstrap.rarities();
    }

    EditorBootstrap bootstrap() {
        return bootstrap;
    }

    public void updateMetadata(
            boolean enabled,
            ResourceLocation rarity,
            List<String> descriptions,
            boolean showGeneratedEffects,
            List<ResourceLocation> categories,
            Optional<ResourceLocation> icon,
            int capacityCost
    ) {
        draft.updateMetadata(
                enabled, rarity, descriptions, showGeneratedEffects, categories, icon, capacityCost
        );
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.metadata_updated").getString();
    }

    public void updateOffer(boolean enabled, double weight, List<ResourceLocation> pools) {
        draft.updateOffer(enabled, weight, pools);
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.offer_updated").getString();
    }

    public TalentDraft.RequirementsDraft requirements() {
        return draft.requirements();
    }

    public void updateRequirements(
            TalentDraft.RequirementMode mode,
            List<TalentDraft.RequirementRow> rows
    ) {
        draft.updateRequirements(mode, rows);
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.requirements_updated").getString();
    }

    public TalentDraft.SynthesisDraft synthesis() {
        return draft.synthesis();
    }

    public void updateSynthesis(
            boolean enabled,
            List<ResourceLocation> talents,
            java.util.Map<ResourceLocation, Long> counters,
            java.util.Map<ResourceLocation, Integer> items
    ) {
        draft.updateSynthesis(enabled, talents, counters, items);
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.synthesis_updated").getString();
    }

    void applyTriggerDraft(TalentDraft working, int selectedTriggerRawIndex) {
        draft.replaceTriggersFrom(working);
        selectedRawIndex = selectedTriggerRawIndex < 0
                ? -1 : TalentDraft.eventSelectionKey(selectedTriggerRawIndex);
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.triggers_updated").getString();
        syncSelectionWidgets();
    }

    TalentDraft draft() {
        return draft;
    }

    void markComplexUpdated(String translationKey) {
        selectedRawIndex = -1;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(translationKey).getString();
    }

    void markEffectUpdated(int rawIndex, String translationKey) {
        selectedRawIndex = rawIndex;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(translationKey).getString();
        syncSelectionWidgets();
    }

    TalentDraft.PresentationDraft presentation(TalentDraft.PresentationPhase phase) {
        return draft.presentation(phase);
    }

    void updatePresentations(
            TalentDraft.PresentationDraft acquire,
            TalentDraft.PresentationDraft synthesize
    ) {
        if (acquire.editable()) draft.updatePresentation(TalentDraft.PresentationPhase.ACQUIRE, acquire);
        if (synthesize.editable()) draft.updatePresentation(TalentDraft.PresentationPhase.SYNTHESIZE, synthesize);
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.presentation_updated").getString();
    }

    private void select(TalentDraft.EffectRow row) {
        selectedRawIndex = row.rawIndex();
        syncSelectionWidgets();
    }

    private void syncSelectionWidgets() {
        TalentDraft.EffectRow selected = selectedRow();
        boolean editable = isEditableEffect(selected);
        if (editEffectButton != null) editEffectButton.active = editable;
        if (removeButton != null) removeButton.active = editable;
    }

    private TalentDraft.EffectRow selectedRow() {
        return draft.allEffectRows().stream()
                .filter(row -> row.rawIndex() == selectedRawIndex)
                .findFirst()
                .orElse(null);
    }

    String effectLabel(TalentDraft.EffectRow row) {
        if (TalentDraft.isEventEffect(row)) {
            String eventKey = "screen.talent_editor.trigger.event." + row.dynamicSource();
            String localizedEvent = Component.translatable(eventKey).getString();
            if (localizedEvent.equals(eventKey)) localizedEvent = row.dynamicSource();
            String label = Component.translatable("screen.talent_editor.event_effect_row",
                    row.idOrType(), localizedEvent).getString();
            return draft.isEditableEventEffect(row) ? label : "[JSON] " + label;
        }
        if (row.editableAttribute()) {
            return row.idOrType() + "   " + signed(row.amount()) + "   " + operationText(row.operation());
        }
        if (row.editableImmunity()) {
            return Component.translatable("screen.talent_editor.immunity_effect", row.idOrType()).getString();
        }
        if (row.editableMaintainedEffect()) {
            return Component.translatable("screen.talent_editor.maintained_effect",
                    row.idOrType(), (int) row.amount() + 1).getString();
        }
        if (row.editableDrawRefreshBonus()) {
            return Component.translatable("screen.talent_editor.refresh_bonus", (int) row.amount()).getString();
        }
        if (row.editableDynamicAttribute()) {
            return Component.translatable("screen.talent_editor.dynamic_attribute_effect",
                    row.idOrType(), Component.translatable(
                            "screen.talent_editor.dynamic.source." + row.dynamicSource()).getString()).getString();
        }
        if (row.editableCreativeFlight()) {
            return Component.translatable("screen.talent_editor.creative_flight_effect").getString();
        }
        return Component.translatable("screen.talent_editor.unsupported_effect", row.idOrType()).getString();
    }

    private String operationText(String operation) {
        return Component.translatable("screen.talent_editor.operation." + operation).getString();
    }

    private void openEffectEditor(int rawIndex) {
        if (minecraft == null) return;
        captureHeaderFields();
        minecraft.setScreen(new TalentEffectEditorScreen(this, bootstrap, rawIndex));
    }

    private void editSelectedEffect() {
        TalentDraft.EffectRow selected = selectedRow();
        if (!isEditableEffect(selected)) return;
        if (TalentDraft.isEventEffect(selected)) {
            openTriggerSettings(this, TalentDraft.eventRawIndex(selected));
        } else {
            openEffectEditor(selected.rawIndex());
        }
    }

    private boolean isEditableEffect(TalentDraft.EffectRow row) {
        return row != null && (row.editableAttribute() || row.editableImmunity()
                || row.editableMaintainedEffect() || row.editableDrawRefreshBonus()
                || row.editableDynamicAttribute() || row.editableCreativeFlight()
                || draft.isEditableEventEffect(row));
    }

    void openOfferSettings() {
        if (minecraft == null) return;
        captureHeaderFields();
        minecraft.setScreen(new TalentOfferScreen(this));
    }

    void openMetadataSettings() {
        if (minecraft == null) return;
        captureHeaderFields();
        minecraft.setScreen(new TalentMetadataScreen(this));
    }

    void openRequirementsSettings() {
        if (minecraft == null) return;
        captureHeaderFields();
        minecraft.setScreen(new TalentRequirementsScreen(this));
    }

    void openSynthesisSettings() {
        if (minecraft == null) return;
        captureHeaderFields();
        minecraft.setScreen(new TalentSynthesisScreen(this));
    }

    void openTriggerSettings(Screen returnScreen) {
        openTriggerSettings(returnScreen, -1);
    }

    void openTriggerSettings(Screen returnScreen, int selectedTriggerRawIndex) {
        if (minecraft == null) return;
        captureHeaderFields();
        TalentDraft working = TalentDraft.parse(draft.toJson(nameValue));
        minecraft.setScreen(new TalentTriggerScreen(
                this, working, bootstrap, returnScreen, selectedTriggerRawIndex
        ));
    }

    void openPresentationSettings() {
        if (minecraft == null) return;
        captureHeaderFields();
        minecraft.setScreen(new TalentPresentationScreen(this, bootstrap));
    }

    void openTextSettings() {
        if (minecraft == null) return;
        captureHeaderFields();
        draft.prepareNameLiteral(nameValue);
        minecraft.setScreen(new TalentTextStyleScreen(this));
    }

    private void openSettingsMenu() {
        if (minecraft == null) return;
        captureHeaderFields();
        minecraft.setScreen(new TalentSettingsMenuScreen(this));
    }

    private void openReferences() {
        if (!existing || minecraft == null) return;
        captureHeaderFields();
        ResourceLocation id = ResourceLocation.tryParse(idValue.strip());
        if (id == null) return;
        int count = DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.TALENT, id);
        minecraft.setScreen(new DefinitionReferenceScreen(this, bootstrap, DefinitionKind.TALENT, id, count));
    }

    private void removeSelected() {
        TalentDraft.EffectRow selected = selectedRow();
        if (!isEditableEffect(selected)) return;
        if (TalentDraft.isEventEffect(selected)) {
            draft.removeEffectTrigger(TalentDraft.eventRawIndex(selected));
        } else {
            draft.removeEditableActiveEffect(selected.rawIndex());
        }
        selectedRawIndex = -1;
        syncSelectionWidgets();
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.effect_removed").getString();
    }

    private void submit(boolean commit) {
        captureHeaderFields();
        ResourceLocation id = ResourceLocation.tryParse(idValue.strip());
        if (id == null) {
            ClientEditorState.clearValidation();
            localStatus = Component.translatable("screen.talent_editor.form.invalid_id").getString();
            return;
        }
        if (nameValue.isBlank()) {
            ClientEditorState.clearValidation();
            localStatus = Component.translatable("screen.talent_editor.form.empty_name").getString();
            return;
        }
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        EditorNetwork.submitTalent(baseRevision, id, draft.toJson(nameValue), commit);
    }

    private void captureHeaderFields() {
        if (idField != null) idValue = idField.getValue();
        if (nameField != null) nameValue = nameField.getValue();
    }

    private void returnHome() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new EditorHomeScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnHome();
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.8f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }

    private static String signed(double value) {
        return value >= 0 ? "+" + compact(value) : compact(value);
    }
}
