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
import java.util.Map;
import java.util.Optional;

/** Unified, key-binding-style entry point for every visual talent setting. */
public final class TalentEditScreen extends Screen {
    private final EditorBootstrap bootstrap;
    private final TalentDraft draft;
    private final boolean returnToTalentTree;
    private boolean existing;
    private long baseRevision;
    private String idValue;
    private String nameValue;
    private EditBox idField;
    private EditBox nameField;
    private TalentEditOptionList overviewList;
    private double overviewScroll;
    private String overviewSelectedKey = "";
    private String localStatus = "";

    private TalentEditScreen(EditorBootstrap bootstrap, TalentDraft draft, ResourceLocation id, boolean existing) {
        this(bootstrap, draft, id, existing, false);
    }

    private TalentEditScreen(
            EditorBootstrap bootstrap,
            TalentDraft draft,
            ResourceLocation id,
            boolean existing,
            boolean returnToTalentTree
    ) {
        super(Component.translatable(existing
                ? "screen.talent_editor.edit.title"
                : "screen.talent_editor.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.returnToTalentTree = returnToTalentTree;
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

    static Screen editExistingFromTree(
            EditorBootstrap bootstrap,
            ResourceLocation id,
            String json,
            TalentTreeEditRequest request
    ) {
        TalentEditScreen owner = new TalentEditScreen(
                bootstrap, TalentDraft.parse(json), id, true, true
        );
        TalentRelationMutation.Result result = TalentRelationMutation.apply(owner.draft, request);
        if (request.mutation() != TalentTreeEditRequest.Mutation.NONE) {
            owner.localStatus = Component.translatable(
                    "screen.talent_editor.tree.mutation." + result.name().toLowerCase(Locale.ROOT)
            ).getString();
        }
        return switch (request.page()) {
            case TALENT -> owner;
            case ACQUISITION -> new TalentRequirementsScreen(owner, owner);
            case SYNTHESIS -> new TalentSynthesisScreen(owner, owner);
        };
    }

    @Override
    protected void init() {
        int formWidth = Math.min(720, width - 32);
        int x = (width - formWidth) / 2;
        idField = new EditBox(font, x, 43, formWidth, 20,
                Component.translatable("screen.talent_editor.form.id"));
        idField.setMaxLength(256);
        idField.setValue(idValue);
        idField.setEditable(!existing);
        addRenderableWidget(idField);

        nameField = new EditBox(font, x, 76, formWidth, 20,
                Component.translatable("screen.talent_editor.form.name"));
        nameField.setMaxLength(512);
        nameField.setValue(nameValue);
        addRenderableWidget(nameField);

        overviewList = new TalentEditOptionList(minecraft, width, height, 108, height - 57);
        populateOverview();
        overviewList.setScrollAmount(overviewScroll);
        overviewList.restoreSelectedKey(overviewSelectedKey);
        addRenderableWidget(overviewList);

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
    }

    private void populateOverview() {
        TalentDraft.EditorOverview overview = draft.editorOverview();
        header("display");
        option(TalentDraft.EditorSection.TEXT, overview, this::openTextSettings);
        option(TalentDraft.EditorSection.METADATA, overview, this::openMetadataSettings);
        option(TalentDraft.EditorSection.PRESENTATION, overview, this::openPresentationSettings);

        header("progression");
        option(TalentDraft.EditorSection.OFFER, overview, this::openOfferSettings);
        option(TalentDraft.EditorSection.REQUIREMENTS, overview, this::openRequirementsSettings);
        option(TalentDraft.EditorSection.SYNTHESIS, overview, this::openSynthesisSettings);
        option(TalentDraft.EditorSection.LIFECYCLE, overview, this::openLifecycleSettings);

        header("effects");
        option(TalentDraft.EditorSection.BASIC_EFFECTS, overview, this::openBasicEffects);
        option(TalentDraft.EditorSection.SPECIAL_EFFECTS, overview, this::openSpecialEffects);
        option(TalentDraft.EditorSection.ACTIVE_INTERFACES, overview,
                () -> openActions(TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS));

        header("events");
        option(TalentDraft.EditorSection.TRIGGERS, overview, () -> openTriggerSettings(this));
        option(TalentDraft.EditorSection.ON_ACQUIRE_ACTIONS, overview,
                () -> openActions(TalentDraft.AdvancedActionLocation.ON_ACQUIRE));
        option(TalentDraft.EditorSection.ON_SYNTHESIZE_ACTIONS, overview,
                () -> openActions(TalentDraft.AdvancedActionLocation.ON_SYNTHESIZE));
        option(TalentDraft.EditorSection.ON_REMOVE_ACTIONS, overview,
                () -> openActions(TalentDraft.AdvancedActionLocation.ON_REMOVE));

        header("advanced");
        option(TalentDraft.EditorSection.RAW_JSON, overview, this::openRawJson);
    }

    private void header(String key) {
        overviewList.addHeader(Component.translatable("screen.talent_editor.overview.group." + key));
    }

    private void option(
            TalentDraft.EditorSection section,
            TalentDraft.EditorOverview overview,
            Runnable action
    ) {
        TalentDraft.EditorSectionState state = overview.section(section);
        String key = section.name().toLowerCase(Locale.ROOT);
        overviewList.addOption(
                key,
                Component.translatable("screen.talent_editor.overview.item." + key),
                overviewSummary(section, state),
                state.configured(),
                state.advancedOnly(),
                action
        );
    }

    private Component overviewSummary(TalentDraft.EditorSection section, TalentDraft.EditorSectionState state) {
        String prefix = "screen.talent_editor.overview.summary.";
        return switch (section) {
            case TEXT -> state.configured()
                    ? Component.translatable(prefix + "text", state.count())
                    : Component.translatable(prefix + "default");
            case METADATA -> state.configured()
                    ? Component.translatable(prefix + "metadata", state.count())
                    : Component.translatable(prefix + "metadata_default");
            case PRESENTATION -> countSummary(prefix, "presentation", state);
            case OFFER -> offerSummary(prefix);
            case REQUIREMENTS -> state.advancedOnly()
                    ? Component.translatable(prefix + "advanced")
                    : state.count() == 0
                    ? Component.translatable(prefix + "none")
                    : Component.translatable(prefix + "requirements", state.count(),
                    Component.translatable("screen.talent_editor.tree.mode."
                            + requirements().mode().name().toLowerCase(Locale.ROOT)));
            case SYNTHESIS -> synthesis().enabled()
                    ? Component.translatable(prefix + "synthesis", state.count())
                    : state.count() > 0
                    ? Component.translatable(prefix + "synthesis_disabled", state.count())
                    : Component.translatable(prefix + "disabled");
            case LIFECYCLE -> state.configured()
                    ? Component.translatable(prefix + "lifecycle",
                    lifecycle().durationTicks(), lifecycle().removeAfterDraws())
                    : Component.translatable(prefix + "permanent");
            case BASIC_EFFECTS, SPECIAL_EFFECTS, ACTIVE_INTERFACES -> countSummary(prefix, "effects", state);
            case TRIGGERS -> countSummary(prefix, "triggers", state);
            case ON_ACQUIRE_ACTIONS, ON_SYNTHESIZE_ACTIONS, ON_REMOVE_ACTIONS ->
                    countSummary(prefix, "actions", state);
            case RAW_JSON -> state.configured()
                    ? Component.translatable(prefix + "advanced")
                    : Component.translatable(prefix + "json_fallback");
        };
    }

    private Component offerSummary(String prefix) {
        if (!draft.hasOfferDefinition()) return Component.translatable(prefix + "offer_legacy_disabled");
        TalentDraft.OfferDraft value = offer();
        if (!value.enabled()) return Component.translatable(prefix + "disabled");
        Component pools = value.pools().isEmpty()
                ? Component.translatable(prefix + "all_pools")
                : value.pools().equals(List.of(new ResourceLocation("talent_editor", "default")))
                ? Component.translatable(prefix + "default_pool")
                : Component.translatable(prefix + "pool_count", value.pools().size());
        return Component.translatable(prefix + "offer", compact(value.weight()), pools);
    }

    private static Component countSummary(
            String prefix,
            String kind,
            TalentDraft.EditorSectionState state
    ) {
        return state.count() == 0
                ? Component.translatable(prefix + "none")
                : Component.translatable(prefix + kind, state.count());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(720, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, 75);
        EditorTheme.panel(graphics, x - 7, 104, formWidth + 14, Math.max(1, height - 160));
        EditorTheme.panel(graphics, x - 7, height - 53, formWidth + 14, 48);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawString(font, Component.translatable("screen.talent_editor.form.id"), x, 32,
                EditorTheme.MUTED, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.form.name"), x, 65,
                EditorTheme.MUTED, false);
        renderResult(graphics, x, formWidth);
    }

    private void renderResult(GuiGraphics graphics, int x, int formWidth) {
        ValidationResultPacket result = ClientEditorState.validation();
        String message = localStatus;
        int color = EditorTheme.MUTED;
        if (result != null) {
            if (result.committed()) {
                message = Component.translatable("screen.talent_editor.result.saved", result.activeRevision()).getString();
                color = 0xFF55FF55;
                if (result.activeRevision() > baseRevision) {
                    baseRevision = result.activeRevision();
                    existing = true;
                    idField.setEditable(false);
                }
            } else if (result.accepted()) {
                message = Component.translatable("screen.talent_editor.result.valid").getString();
                color = 0xFF55FF55;
            } else if (result.conflict()) {
                message = Component.translatable("screen.talent_editor.result.conflict",
                        result.activeRevision()).getString();
                color = 0xFFFF5555;
            } else {
                ValidationIssue first = result.issues().isEmpty() ? null : result.issues().get(0);
                message = first == null
                        ? Component.translatable("screen.talent_editor.result.invalid").getString()
                        : first.code() + " " + first.path() + ": " + first.message();
                color = 0xFFFF7777;
            }
        }
        if (!message.isBlank()) graphics.drawString(font,
                font.plainSubstrByWidth(message, formWidth), x, height - 47, color, false);
    }

    public void addAttribute(ResourceLocation attribute) {
        addAttributeEffect(attribute);
    }

    int addAttributeEffect(ResourceLocation attribute) {
        captureOverviewState();
        int rawIndex = draft.addAttribute(attribute);
        changed(Component.translatable("screen.talent_editor.attribute_added", attribute).getString());
        return rawIndex;
    }

    int addEffectImmunity(ResourceLocation effect) {
        captureOverviewState();
        int rawIndex = draft.addEffectImmunity(effect);
        changed(Component.translatable("screen.talent_editor.immunity_added", effect).getString());
        return rawIndex;
    }

    int addMaintainedEffect(ResourceLocation effect) {
        captureOverviewState();
        int rawIndex = draft.addMaintainedEffect(effect);
        changed(Component.translatable("screen.talent_editor.maintained_effect_added", effect).getString());
        return rawIndex;
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
        changed(Component.translatable("screen.talent_editor.text_updated").getString());
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
            boolean showGeneratedEffects,
            List<ResourceLocation> categories,
            Optional<ResourceLocation> icon,
            int capacityCost
    ) {
        draft.updateMetadata(enabled, rarity, showGeneratedEffects, categories, icon, capacityCost);
        changed(Component.translatable("screen.talent_editor.metadata_updated").getString());
    }

    public void updateOffer(boolean enabled, double weight, List<ResourceLocation> pools) {
        draft.updateOffer(enabled, weight, pools);
        changed(Component.translatable("screen.talent_editor.offer_updated").getString());
    }

    public TalentDraft.RequirementsDraft requirements() {
        return draft.requirements();
    }

    public void updateRequirements(TalentDraft.RequirementMode mode, List<TalentDraft.RequirementRow> rows) {
        draft.updateRequirements(mode, rows);
        changed(Component.translatable("screen.talent_editor.requirements_updated").getString());
    }

    public TalentDraft.SynthesisDraft synthesis() {
        return draft.synthesis();
    }

    public void updateSynthesis(
            boolean enabled,
            List<ResourceLocation> talents,
            Map<ResourceLocation, Long> counters,
            Map<ResourceLocation, Integer> items
    ) {
        draft.updateSynthesis(enabled, talents, counters, items);
        changed(Component.translatable("screen.talent_editor.synthesis_updated").getString());
    }

    void applyTriggerDraft(TalentDraft working, int selectedTriggerRawIndex) {
        draft.replaceTriggersFrom(working);
        changed(Component.translatable("screen.talent_editor.triggers_updated").getString());
    }

    TalentDraft draft() {
        return draft;
    }

    void markComplexUpdated(String translationKey) {
        changed(Component.translatable(translationKey).getString());
    }

    void markEffectUpdated(int rawIndex, String translationKey) {
        changed(Component.translatable(translationKey).getString());
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

    private void openBasicEffects() {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentEffectEditorScreen(this, bootstrap, -1));
    }

    private void openSpecialEffects() {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentComplexEffectsScreen(this, this, bootstrap));
    }

    private void openActions(TalentDraft.AdvancedActionLocation location) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentAdvancedActionListScreen(this, this, location, -1));
    }

    void openOfferSettings() {
        openOfferSettings(this);
    }

    void openOfferSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentOfferScreen(this, returnScreen));
    }

    void openMetadataSettings() {
        openMetadataSettings(this);
    }

    void openMetadataSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentMetadataScreen(this, returnScreen));
    }

    void openRequirementsSettings() {
        openRequirementsSettings(this);
    }

    void openRequirementsSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentRequirementsScreen(this, returnScreen));
    }

    void openSynthesisSettings() {
        openSynthesisSettings(this);
    }

    void openSynthesisSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentSynthesisScreen(this, returnScreen));
    }

    void openTriggerSettings(Screen returnScreen) {
        openTriggerSettings(returnScreen, -1);
    }

    void openTriggerSettings(Screen returnScreen, int selectedTriggerRawIndex) {
        if (minecraft == null) return;
        captureOverviewState();
        TalentDraft working = TalentDraft.parse(draft.toJson(nameValue));
        minecraft.setScreen(new TalentTriggerScreen(
                this, working, bootstrap, returnScreen, selectedTriggerRawIndex
        ));
    }

    void openPresentationSettings() {
        openPresentationSettings(this);
    }

    void openPresentationSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentPresentationTimelineScreen(returnScreen, this,
                TalentDraft.PresentationPhase.ACQUIRE));
    }

    void presentationApplied() {
        changed(Component.translatable("screen.talent_editor.presentation_updated").getString());
    }

    void openLifecycleSettings() {
        openLifecycleSettings(this);
    }

    void openLifecycleSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentLifecycleScreen(this, returnScreen));
    }

    void openAdvancedJsonSettings() {
        openRawJson();
    }

    void openAdvancedJsonSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        minecraft.setScreen(new TalentRawJsonScreen(returnScreen, this, currentTalentJson()));
    }

    private void openRawJson() {
        openAdvancedJsonSettings(this);
    }

    TalentDraft.LifecycleDraft lifecycle() {
        return draft.lifecycle();
    }

    void applyLifecycle(TalentDraft.LifecycleDraft lifecycle) {
        draft.updateLifecycle(lifecycle);
        changed(Component.translatable("screen.talent_editor.lifecycle.updated").getString());
    }

    void applyAdvancedJson(String json) {
        TalentDraft replacement = TalentDraft.parse(json);
        draft.replaceAllFrom(replacement);
        nameValue = replacement.name();
        changed(Component.translatable("screen.talent_editor.advanced_json.updated").getString());
    }

    String currentTalentJson() {
        captureHeaderFields();
        return draft.toJson(nameValue);
    }

    void openTextSettings() {
        openTextSettings(this);
    }

    void openTextSettings(Screen returnScreen) {
        if (minecraft == null) return;
        captureOverviewState();
        draft.prepareNameLiteral(nameValue);
        minecraft.setScreen(new TalentTextStyleScreen(this, returnScreen));
    }

    private void openReferences() {
        if (!existing || minecraft == null) return;
        captureOverviewState();
        ResourceLocation id = ResourceLocation.tryParse(idValue.strip());
        if (id == null) return;
        int count = DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.TALENT, id);
        minecraft.setScreen(new DefinitionReferenceScreen(this, bootstrap, DefinitionKind.TALENT, id, count));
    }

    private void submit(boolean commit) {
        captureOverviewState();
        ResourceLocation id = ResourceLocation.tryParse(idValue.strip());
        if (id == null) {
            changed(Component.translatable("screen.talent_editor.form.invalid_id").getString());
            return;
        }
        if (nameValue.isBlank()) {
            changed(Component.translatable("screen.talent_editor.form.empty_name").getString());
            return;
        }
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        EditorNetwork.submitTalent(baseRevision, id, draft.toJson(nameValue), commit);
    }

    private void changed(String status) {
        ClientEditorState.clearValidation();
        localStatus = status;
    }

    private void captureHeaderFields() {
        if (idField != null) idValue = idField.getValue();
        if (nameField != null) nameValue = nameField.getValue();
    }

    private void captureOverviewState() {
        captureHeaderFields();
        if (overviewList != null) {
            overviewScroll = overviewList.getScrollAmount();
            overviewSelectedKey = overviewList.selectedKey();
        }
    }

    private void returnHome() {
        if (minecraft == null) return;
        if (returnToTalentTree) {
            ClientEditorState.openTalentTreeOnNextBootstrap();
            EditorNetwork.requestOpen();
            return;
        }
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
