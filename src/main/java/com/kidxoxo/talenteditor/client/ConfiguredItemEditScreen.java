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

public final class ConfiguredItemEditScreen extends Screen {
    private static final String MOD_ID_PATTERN = "[a-z][a-z0-9_-]{1,63}";

    private final EditorBootstrap bootstrap;
    private final ConfiguredItemDraft draft;
    private ResourceLocation definitionId;
    private boolean existing;
    private long baseRevision;
    private boolean enabled;
    private boolean glint;
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
    private String localStatus = "";

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

    @Override
    protected void init() {
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        idField = field(x, 30, formWidth, idValue, 256, value -> idValue = value);
        idField.setEditable(!existing);
        nameField = field(x, 58, formWidth, nameValue, 512, value -> nameValue = value);
        descriptionField = field(x, 86, formWidth, descriptionValue, 4096,
                value -> descriptionValue = value);
        int gap = 6;
        int half = (formWidth - gap) / 2;
        modelField = field(x, 114, half, modelValue, 8, value -> modelValue = value);
        modsField = field(x + half + gap, 114, formWidth - half - gap, modsValue, 2048,
                value -> modsValue = value);
        int toggleWidth = (formWidth - gap * 3) / 4;
        addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            ClientEditorState.clearValidation();
        }).bounds(x, 142, toggleWidth, 20).build());
        addRenderableWidget(Button.builder(glintLabel(), button -> {
            glint = !glint;
            button.setMessage(glintLabel());
            ClientEditorState.clearValidation();
        }).bounds(x + toggleWidth + gap, 142, toggleWidth, 20).build());
        addRenderableWidget(Button.builder(attributesLabel(), button -> openAttributeModifiers())
                .bounds(x + (toggleWidth + gap) * 2, 142, toggleWidth, 20).build());
        addRenderableWidget(Button.builder(lootLabel(), button -> openLootRules())
                .bounds(x + (toggleWidth + gap) * 3, 142,
                        formWidth - (toggleWidth + gap) * 3, 20).build());
        Button useButton = addRenderableWidget(Button.builder(useLabel(), button -> openUseSettings())
                .bounds(x, 168, half, 20).build());
        useButton.active = !ConfiguredItem.isSystemDefinitionId(definitionId);
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.configured_item.import_icon"),
                        button -> openIconImport())
                .bounds(x + half + gap, 168, formWidth - half - gap, 20).build());

        int bottomY = height - 28;
        int buttonWidth = Math.max(54, (formWidth - gap * 3) / 4);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnList())
                .bounds(x, bottomY, buttonWidth, 20).build());
        referencesButton = addRenderableWidget(Button.builder(
                        DefinitionReferenceScreen.buttonLabel(DefinitionReferenceScreen.inboundCount(
                                bootstrap, DefinitionKind.ITEM, definitionId
                        )), button -> openReferences())
                .bounds(x + buttonWidth + gap, bottomY, buttonWidth, 20).build());
        referencesButton.active = existing;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.validate"),
                        button -> submit(false))
                .bounds(x + (buttonWidth + gap) * 2, bottomY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.save"),
                        button -> submit(true))
                .bounds(x + (buttonWidth + gap) * 3, bottomY,
                        formWidth - (buttonWidth + gap) * 3, 20).build());
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
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        int half = (formWidth - 6) / 2;
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        label(graphics, "screen.talent_editor.configured_item.id", x, 19);
        label(graphics, "screen.talent_editor.configured_item.name", x, 47);
        label(graphics, "screen.talent_editor.configured_item.description", x, 75);
        label(graphics, "screen.talent_editor.configured_item.model", x, 103);
        label(graphics, "screen.talent_editor.configured_item.required_mods", x + half + 6, 103);
        if (height >= 280) {
            graphics.drawString(font, font.plainSubstrByWidth(
                    Component.translatable("screen.talent_editor.configured_item.help").getString(), formWidth
            ), x, 198, 0x808080, false);
        }
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
                    ResourceLocation saved = ResourceLocation.tryParse(idField.getValue().strip());
                    if (saved != null) definitionId = saved;
                    idField.setEditable(false);
                    if (referencesButton != null) referencesButton.active = true;
                }
            } else if (result.accepted()) {
                message = Component.translatable("screen.talent_editor.result.valid").getString();
                color = 0x55FF55;
            } else if (result.conflict()) {
                message = Component.translatable(
                        "screen.talent_editor.result.conflict", result.activeRevision()
                ).getString();
                color = 0xFF5555;
            } else {
                ValidationIssue first = result.issues().isEmpty() ? null : result.issues().get(0);
                message = first == null ? Component.translatable("screen.talent_editor.result.invalid").getString()
                        : first.code() + " " + first.path() + ": " + first.message();
                color = 0xFF7777;
            }
        }
        if (!message.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(message, formWidth),
                    x, height - 47, color, false);
        }
    }

    private void submit(boolean commit) {
        Values values = readValues();
        if (values == null) return;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        EditorNetwork.submitConfiguredItem(baseRevision, values.id(), draft.toJson(
                values.name(), descriptionField.getValue(), enabled, values.customModelData(), glint,
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
        if (!existing || minecraft == null) return;
        minecraft.setScreen(new DefinitionReferenceScreen(
                this, bootstrap, DefinitionKind.ITEM, definitionId,
                DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.ITEM, definitionId)
        ));
    }

    private void returnList() {
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
}
