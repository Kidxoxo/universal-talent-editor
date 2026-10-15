package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public final class ConfiguredItemEditScreen extends EditorDefinitionScreen {
    private static final String MOD_ID_PATTERN = "[a-z][a-z0-9_-]{1,63}";

    private final EditorBootstrap bootstrap;
    private final ConfiguredItemDraft draft;
    private ResourceLocation definitionId;
    private boolean existing;
    private long baseRevision;
    private boolean enabled;
    private boolean glint;
    private boolean curiosEquippable;
    private List<String> curiosSlots;
    private boolean mainhandOnly;
    private String idValue;
    private String nameValue;
    private String descriptionValue;
    private String modelValue;
    private String modsValue;
    private EditBox idField;
    private EditBox nameField;
    private EditBox descriptionField;
    private EditBox modelField;
    private EditBox modsField;
    private Button referencesButton;
    private Button curiosButton;
    private String localStatus = "";
    private final EditorOptionPopup itemOptions = new EditorOptionPopup();

    private ConfiguredItemEditScreen(
            EditorBootstrap bootstrap,
            ConfiguredItemDraft draft,
            ResourceLocation id,
            boolean existing
    ) {
        super(Component.translatable(existing
                ? "screen.talent_editor.configured_item.edit.title"
                : "screen.talent_editor.configured_item.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.definitionId = id;
        this.existing = existing;
        this.baseRevision = bootstrap.revision();
        this.enabled = draft.enabled();
        this.glint = draft.glint();
        this.curiosEquippable = draft.curiosEquippable();
        this.curiosSlots = draft.curiosSlots();
        this.mainhandOnly = draft.mainhandOnly();
        this.idValue = id.toString();
        this.nameValue = draft.name();
        this.descriptionValue = draft.descriptionEditorText();
        this.modelValue = Integer.toString(draft.customModelData());
        this.modsValue = draft.requiredMods();
    }

    public static ConfiguredItemEditScreen createNew(EditorBootstrap bootstrap) {
        return new ConfiguredItemEditScreen(
                bootstrap, ConfiguredItemDraft.create(), new ResourceLocation("custom", "new_item"), false
        );
    }

    public static ConfiguredItemEditScreen editExisting(
            EditorBootstrap bootstrap, ResourceLocation id, String json
    ) {
        return new ConfiguredItemEditScreen(bootstrap, ConfiguredItemDraft.parse(json), id, true);
    }

    static ConfiguredItemEditScreen copyOf(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new ConfiguredItemEditScreen(bootstrap,ConfiguredItemDraft.parse(json),id,false);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        int rowStep = height >= 280 ? 34 : 30;
        int toggleY = 38 + rowStep * 4;
        int useY = toggleY + 26;
        idField = field(x, 38, formWidth, idValue, 256, value -> idValue = value);
        idField.setEditable(!existing);
        nameField = field(x, 38 + rowStep, formWidth, nameValue, 512, value -> nameValue = value);
        descriptionField = field(x, 38 + rowStep * 2, formWidth, descriptionValue, 4096,
                value -> descriptionValue = value);
        int gap = 6;
        int half = (formWidth - gap) / 2;
        modelField = field(x, 38 + rowStep * 3, half, modelValue, 8, value -> modelValue = value);
        modsField = field(x + half + gap, 38 + rowStep * 3, formWidth - half - gap, modsValue, 2048,
                value -> modsValue = value);
        int toggleWidth = (formWidth - gap * 3) / 4;
        addRenderableWidget(EditorWidgets.button(enabledLabel(), button -> {enabled = !enabled; button.setMessage(enabledLabel());})
                .bounds(x, toggleY, toggleWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("item_appearance"), button -> itemOptions.open(
                EditorDefinitionScreen.tr("item_appearance"), List.of(glintLabel(), Component.translatable("screen.talent_editor.configured_item.import_icon")), 0,
                choice -> {if(choice==0)glint=!glint;else openIconImport();}, height))
                .bounds(x+toggleWidth+gap,toggleY,toggleWidth,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("item_behavior"), button -> itemOptions.open(
                EditorDefinitionScreen.tr("item_behavior"), List.of(attributesLabel(),lootLabel(),useLabel()), 0,
                choice -> {if(choice==0)openAttributeModifiers();else if(choice==1)openLootRules();else if(!ConfiguredItem.isSystemDefinitionId(definitionId))openUseSettings();}, height))
                .bounds(x+(toggleWidth+gap)*2,toggleY,toggleWidth,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("item_advanced"), button -> itemOptions.open(
                EditorDefinitionScreen.tr("item_advanced"), List.of(triggersLabel(),curiosLabel(),mainhandLabel()), 0,
                choice -> {if(choice==0)openTriggers();else if(choice==1)openCuriosSlots();else {mainhandOnly=!mainhandOnly;if(mainhandOnly)curiosEquippable=false;}}, height))
                .bounds(x+(toggleWidth+gap)*3,toggleY,formWidth-(toggleWidth+gap)*3,20).build());

        int bottomY = height - 28;
        int buttonWidth = Math.max(54, (formWidth - gap * 3) / 4);
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnList())
                .bounds(x, bottomY, buttonWidth, 20).build());
        referencesButton = addRenderableWidget(EditorWidgets.button(
                        DefinitionReferenceScreen.buttonLabel(DefinitionReferenceScreen.inboundCount(
                                bootstrap, DefinitionKind.ITEM, definitionId
                        )), button -> definitionActions(bootstrap,ResourceLocation.tryParse(idField.getValue().strip()),existing,this::openReferences))
                .bounds(x + buttonWidth + gap, bottomY, buttonWidth, 20).build());
        referencesButton.active = true;
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.validate"),
                        button -> submit(false))
                .bounds(x + (buttonWidth + gap) * 2, bottomY, buttonWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.save"), EditorTheme.Tone.PRIMARY,
                        button -> submit(true))
                .bounds(x + (buttonWidth + gap) * 3, bottomY,
                        formWidth - (buttonWidth + gap) * 3, 20).build());
        watchDefinition(DefinitionKind.ITEM);
    }

    private EditBox field(
            int x, int y, int width, String value, int maxLength,
            java.util.function.Consumer<String> responder
    ) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(ignored -> {
            responder.accept(ignored);
            ClientEditorState.clearValidation();
            localStatus = "";
        });
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 8, 24, formWidth + 16, Math.min(224, height - 60));
        super.render(graphics, mouseX, mouseY, partialTick);
        int half = (formWidth - 6) / 2;
        int rowStep = height >= 280 ? 34 : 30;
        EditorTheme.title(graphics,font,title,width);
        label(graphics, "screen.talent_editor.configured_item.id", x, 27);
        label(graphics, "screen.talent_editor.configured_item.name", x, 28 + rowStep);
        label(graphics, "screen.talent_editor.configured_item.description", x, 28 + rowStep * 2);
        label(graphics, "screen.talent_editor.configured_item.model", x, 28 + rowStep * 3);
        label(graphics, "screen.talent_editor.configured_item.required_mods", x + half + 6,
                28 + rowStep * 3);
        if (height >= 300) {
            graphics.drawString(font, font.plainSubstrByWidth(
                    Component.translatable("screen.talent_editor.configured_item.help").getString(), formWidth
            ), x, 230, EditorTheme.MUTED, false);
        }
        renderResult(graphics, x, formWidth);
        itemOptions.render(graphics,font,width,height);
    }

    @Override public boolean mouseClicked(double x,double y,int b){return itemOptions.mouseClicked(x,y,b,width,height)||super.mouseClicked(x,y,b);}
    @Override public boolean mouseScrolled(double x,double y,double d){return itemOptions.mouseScrolled(d,height)||super.mouseScrolled(x,y,d);}
    @Override public boolean keyPressed(int k,int scan,int mods){return itemOptions.keyPressed(k,height)||super.keyPressed(k,scan,mods);}
    @Override public boolean charTyped(char c,int mods){return itemOptions.isOpen()||super.charTyped(c,mods);}

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, EditorTheme.MUTED, false);
    }

    private void renderResult(GuiGraphics graphics, int x, int formWidth) {
        renderSubmissionResult(graphics, x, formWidth, localStatus);
    }

    private void submit(boolean commit) {
        if(submissionPending())return;
        Values values = readValues();
        if (values == null) return;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        sendDefinition(baseRevision, values.id(), ConfiguredItemDraft.parse(draft.snapshot()).toJson(
                values.name(), descriptionField.getValue(), enabled, values.customModelData(), glint,
                curiosEquippable, mainhandOnly, curiosSlots,
                values.requiredMods()
        ), commit);
    }

    private Values readValues() {
        String rawId = idField.getValue().strip();
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !rawId.contains(":")) return invalid("screen.talent_editor.configured_item.invalid_id");
        String name = nameField.getValue().strip();
        if (name.isBlank()) return invalid("screen.talent_editor.form.empty_name");
        int customModelData;
        try {
            customModelData = Integer.parseInt(modelField.getValue().strip());
            if (customModelData < 0 || customModelData > 16_777_215) throw new NumberFormatException();
        } catch (NumberFormatException ignored) {
            return invalid("screen.talent_editor.configured_item.invalid_model");
        }
        ArrayList<String> requiredMods = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (String raw : modsField.getValue().split(",")) {
            String mod = raw.strip().toLowerCase(Locale.ROOT);
            if (mod.isBlank()) continue;
            if (!mod.matches(MOD_ID_PATTERN) || !seen.add(mod)) {
                return invalid("screen.talent_editor.configured_item.invalid_mods");
            }
            requiredMods.add(mod);
        }
        return new Values(id, name, customModelData, List.copyOf(requiredMods));
    }

    private Values invalid(String key) {
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(key).getString();
        return null;
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.configured_item.enabled_yes"
                : "screen.talent_editor.configured_item.enabled_no");
    }

    private Component glintLabel() {
        return Component.translatable(glint
                ? "screen.talent_editor.configured_item.glint_yes"
                : "screen.talent_editor.configured_item.glint_no");
    }

    private Component curiosLabel() {
        if (!curiosEquippable || mainhandOnly) return Component.translatable("screen.talent_editor.configured_item.curios_no");
        return curiosSlots.isEmpty()
                ? Component.translatable("screen.talent_editor.configured_item.curios_any")
                : Component.translatable("screen.talent_editor.configured_item.curios_count", curiosSlots.size());
    }

    private void openCuriosSlots() {
        if (minecraft != null) minecraft.setScreen(new ConfiguredItemCuriosSlotsScreen(
                this, curiosEquippable, mainhandOnly, curiosSlots));
    }

    void updateCuriosSlots(boolean enabled, List<String> slots) {
        curiosEquippable = enabled && !mainhandOnly;
        curiosSlots = List.copyOf(slots);
        ClientEditorState.clearValidation();
        localStatus = "";
    }

    private Component mainhandLabel() {
        return Component.translatable(mainhandOnly
                ? "screen.talent_editor.configured_item.mainhand_only"
                : "screen.talent_editor.configured_item.any_equipment");
    }

    private Component triggersLabel() {
        return Component.translatable("screen.talent_editor.configured_item.triggers_button",
                draft.triggers().size());
    }

    private void openTriggers() {
        if (minecraft != null) minecraft.setScreen(new ConfiguredItemTriggerScreen(this, bootstrap,
                draft.triggers()));
    }

    void updateTriggers(List<com.google.gson.JsonObject> triggers) {
        draft.replaceTriggers(triggers);
        ClientEditorState.clearValidation();
        localStatus = "";
    }

    private Component lootLabel() {
        return Component.translatable(
                "screen.talent_editor.configured_item.loot_button", draft.lootRules().size()
        );
    }

    private Component attributesLabel() {
        return Component.translatable(
                "screen.talent_editor.configured_item.attributes_button", draft.attributeModifiers().size()
        );
    }

    private void openAttributeModifiers() {
        if (minecraft != null) minecraft.setScreen(new ConfiguredItemAttributeScreen(
                this, bootstrap, draft.attributeModifiers()
        ));
    }

    void updateAttributeModifiers(List<ConfiguredItemDraft.AttributeModifierDraft> modifiers) {
        draft.replaceAttributeModifiers(modifiers);
        ClientEditorState.clearValidation();
        localStatus = "";
    }

    private Component useLabel() {
        if (ConfiguredItem.isSystemDefinitionId(definitionId)) {
            return Component.translatable("screen.talent_editor.configured_item.use_button_system");
        }
        ConfiguredItemDraft.UseDraft use = draft.use();
        if (!use.editable()) {
            return Component.translatable("screen.talent_editor.configured_item.use_button_read_only");
        }
        return Component.translatable(
                "screen.talent_editor.configured_item.use_button",
                Component.translatable("options." + (use.enabled() ? "on" : "off")), use.actions().size()
        );
    }

    private void openUseSettings() {
        if (minecraft != null) minecraft.setScreen(new ConfiguredItemUseScreen(this, bootstrap, draft.use()));
    }

    private void openIconImport() {
        if (minecraft == null) return;
        ResourceLocation id = ResourceLocation.tryParse(idField.getValue().strip());
        int model;
        try {
            model = Integer.parseInt(modelField.getValue().strip());
        } catch (NumberFormatException ignored) {
            model = 0;
        }
        if (id == null || !idField.getValue().strip().contains(":")) {
            invalid("screen.talent_editor.configured_item.invalid_id");
            return;
        }
        if (model <= 0 || model > 16_777_215) {
            invalid("screen.talent_editor.configured_item.icon_requires_model");
            return;
        }
        minecraft.setScreen(new ClientResourceImportScreen(this, id, model));
    }

    void updateUse(ConfiguredItemDraft.UseDraft use) {
        draft.replaceUse(use);
        ClientEditorState.clearValidation();
        localStatus = "";
    }

    private void openLootRules() {
        if (minecraft != null) minecraft.setScreen(new ConfiguredItemLootScreen(
                this, bootstrap, draft.lootRules()
        ));
    }

    void updateLootRules(List<ConfiguredItemDraft.LootRuleDraft> rules) {
        draft.replaceLootRules(rules);
        ClientEditorState.clearValidation();
        localStatus = "";
    }

    private void openReferences() {
        EditorBootstrap bootstrap=latestBootstrap(this.bootstrap);
        if (!existing || minecraft == null) return;
        minecraft.setScreen(new DefinitionReferenceScreen(
                this, bootstrap, DefinitionKind.ITEM, definitionId,
                DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.ITEM, definitionId)
        ));
    }

    private void returnList() { leaveDefinition(this::leaveNow); }

    private void leaveNow() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new ConfiguredItemListScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnList();
    }

    private record Values(ResourceLocation id, String name, int customModelData, List<String> requiredMods) {
    }

    @Override protected String draftState() { return fieldsState()+enabled+glint+curiosEquippable+curiosSlots+mainhandOnly+draft.snapshot(); }
    @Override protected void saveDraft() { submit(true); }
    @Override protected void committed(ValidationResultPacket value) { baseRevision=value.activeRevision(); existing=true; idField.setEditable(false); var saved=ResourceLocation.tryParse(idField.getValue().strip()); if(saved!=null)definitionId=saved; if(referencesButton!=null)referencesButton.active=true; }
}
