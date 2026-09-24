package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
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

import java.util.Optional;
import java.util.List;

public final class LayoutEditScreen extends Screen {
    private final EditorBootstrap bootstrap;
    private final LayoutDraft draft;
    private ResourceLocation definitionId;
    private boolean existing;
    private long baseRevision;
    private boolean enabled;
    private boolean showGeneratedEffects;
    private List<LayoutDraft.SlotDraft> slots;
    private List<LayoutDraft.PageDraft> pages;
    private String idValue;
    private String backgroundValue;
    private String priorityValue;
    private String panelPercentValue;
    private String columnsValue;
    private String rowsValue;
    private String slotWidthValue;
    private String slotHeightValue;
    private String iconSizeValue;
    private EditBox idField;
    private String localStatus = "";

    private LayoutEditScreen(EditorBootstrap bootstrap, LayoutDraft draft, ResourceLocation id, boolean existing) {
        super(Component.translatable(existing
                ? "screen.talent_editor.layout.edit.title" : "screen.talent_editor.layout.create.title"));
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.definitionId = id;
        this.existing = existing;
        this.baseRevision = bootstrap.revision();
        this.enabled = draft.enabled();
        this.showGeneratedEffects = draft.showGeneratedEffects();
        this.slots = draft.slots();
        this.pages = draft.pages();
        this.idValue = id.toString();
        this.backgroundValue = draft.background();
        this.priorityValue = Integer.toString(draft.priority());
        this.panelPercentValue = Integer.toString(draft.panelPercent());
        this.columnsValue = Integer.toString(draft.columns());
        this.rowsValue = Integer.toString(draft.rows());
        this.slotWidthValue = Integer.toString(draft.slotWidth());
        this.slotHeightValue = Integer.toString(draft.slotHeight());
        this.iconSizeValue = Integer.toString(draft.iconSize());
    }

    public static LayoutEditScreen createNew(EditorBootstrap bootstrap) {
        return new LayoutEditScreen(
                bootstrap, LayoutDraft.create(), new ResourceLocation("custom", "new_layout"), false
        );
    }

    public static LayoutEditScreen editExisting(EditorBootstrap bootstrap, ResourceLocation id, String json) {
        return new LayoutEditScreen(bootstrap, LayoutDraft.parse(json), id, true);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 5;
        idField = field(x, 29, formWidth, idValue, 256, value -> idValue = value);
        idField.setEditable(!existing);
        int chooseWidth = Math.min(80, Math.max(56, formWidth / 7));
        field(x, 57, formWidth - chooseWidth - gap, backgroundValue, 512,
                value -> backgroundValue = value);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.reference_picker.choose"),
                        button -> openBackgroundPicker())
                .bounds(x + formWidth - chooseWidth, 57, chooseWidth, 20).build());

        int quarter = (formWidth - gap * 3) / 4;
        field(x, 89, quarter, priorityValue, 8, value -> priorityValue = value);
        field(x + quarter + gap, 89, quarter, panelPercentValue, 3, value -> panelPercentValue = value);
        field(x + (quarter + gap) * 2, 89, quarter, columnsValue, 2, value -> columnsValue = value);
        field(x + (quarter + gap) * 3, 89, formWidth - (quarter + gap) * 3,
                rowsValue, 2, value -> rowsValue = value);

        int third = (formWidth - gap * 2) / 3;
        field(x, 121, third, slotWidthValue, 3, value -> slotWidthValue = value);
        field(x + third + gap, 121, third, slotHeightValue, 3, value -> slotHeightValue = value);
        field(x + (third + gap) * 2, 121, formWidth - (third + gap) * 2,
                iconSizeValue, 2, value -> iconSizeValue = value);

        int toggleWidth = (formWidth - gap * 2) / 3;
        addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enabled = !enabled;
            button.setMessage(enabledLabel());
            ClientEditorState.clearValidation();
        }).bounds(x, 151, toggleWidth, 20).build());
        addRenderableWidget(Button.builder(effectsLabel(), button -> {
            showGeneratedEffects = !showGeneratedEffects;
            button.setMessage(effectsLabel());
            ClientEditorState.clearValidation();
        }).bounds(x + toggleWidth + gap, 151, toggleWidth, 20).build());
        addRenderableWidget(Button.builder(slotLabel(), button -> openSlotEditor())
                .bounds(x + (toggleWidth + gap) * 2, 151,
                        formWidth - (toggleWidth + gap) * 2, 20).build());
        addRenderableWidget(Button.builder(pageLabel(), button -> openPageEditor())
                .bounds(x, 177, formWidth, 20).build());

        int bottom = height - 28;
        int buttonWidth = Math.max(42, (formWidth - gap * 4) / 5);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnList())
                .bounds(x, bottom, buttonWidth, 20).build());
        Button references = addRenderableWidget(Button.builder(
                        DefinitionReferenceScreen.buttonLabel(DefinitionReferenceScreen.inboundCount(
                                bootstrap, DefinitionKind.LAYOUT, definitionId
                        )), button -> openReferences())
                .bounds(x + buttonWidth + gap, bottom, buttonWidth, 20).build());
        references.active = existing;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.layout.preview"),
                        button -> preview())
                .bounds(x + (buttonWidth + gap) * 2, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.validate"),
                        button -> submit(false))
                .bounds(x + (buttonWidth + gap) * 3, bottom, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.save"),
                        button -> submit(true))
                .bounds(x + (buttonWidth + gap) * 4, bottom,
                        formWidth - (buttonWidth + gap) * 4, 20).build());
    }

    private EditBox field(int x, int y, int width, String value, int maxLength,
                          java.util.function.Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
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
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 5;
        int quarter = (formWidth - gap * 3) / 4;
        int third = (formWidth - gap * 2) / 3;
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        label(graphics, "screen.talent_editor.layout.id", x, 18);
        label(graphics, "screen.talent_editor.layout.background", x, 46);
        renderBackgroundDiagnostic(graphics, x, 46, formWidth);
        label(graphics, "screen.talent_editor.layout.priority", x, 78);
        label(graphics, "screen.talent_editor.layout.panel_percent", x + quarter + gap, 78);
        label(graphics, "screen.talent_editor.layout.columns", x + (quarter + gap) * 2, 78);
        label(graphics, "screen.talent_editor.layout.rows", x + (quarter + gap) * 3, 78);
        label(graphics, "screen.talent_editor.layout.slot_width", x, 110);
        label(graphics, "screen.talent_editor.layout.slot_height", x + third + gap, 110);
        label(graphics, "screen.talent_editor.layout.icon_size", x + (third + gap) * 2, 110);
        if (height >= 265) graphics.drawString(font, Component.translatable("screen.talent_editor.layout.help"),
                x, 204, 0xA0A0A0, false);
        renderResult(graphics, x, formWidth);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    private void renderBackgroundDiagnostic(GuiGraphics graphics, int x, int y, int formWidth) {
        if (minecraft == null) return;
        Component label = Component.translatable("screen.talent_editor.layout.background");
        int diagnosticX = x + font.width(label) + 8;
        int availableWidth = Math.max(1, x + formWidth - diagnosticX);
        ClientResourceDiagnostics.Result result = ClientResourceDiagnostics.inspect(
                backgroundValue, id -> minecraft.getResourceManager().getResource(id).isPresent()
        );
        Component message;
        int color;
        if (result.state() == ClientResourceDiagnostics.State.AVAILABLE) {
            message = Component.translatable("screen.talent_editor.resource.available_short");
            color = 0xFF77DD88;
        } else if (result.state() == ClientResourceDiagnostics.State.MISSING) {
            message = Component.translatable("screen.talent_editor.resource.texture_missing_short");
            color = 0xFFFFAA55;
        } else if (result.state() == ClientResourceDiagnostics.State.INVALID) {
            message = Component.translatable("screen.talent_editor.resource.invalid_short");
            color = 0xFFFF7777;
        } else {
            return;
        }
        graphics.drawString(font, font.plainSubstrByWidth(message.getString(), availableWidth),
                diagnosticX, y, color, false);
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
                    ResourceLocation savedId = ResourceLocation.tryParse(idValue.strip());
                    if (savedId != null) definitionId = savedId;
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

    private void submit(boolean commit) {
        Parsed parsed = parseForm();
        if (parsed == null) return;
        ClientEditorState.clearValidation();
        localStatus = Component.translatable("screen.talent_editor.result.waiting").getString();
        EditorNetwork.submitLayout(baseRevision, parsed.id(), parsed.json(), commit);
    }

    private void preview() {
        Parsed parsed = parseForm();
        if (parsed == null || minecraft == null) return;
        minecraft.setScreen(new LayoutPreviewScreen(this, parsed.definition()));
    }

    private Parsed parseForm() {
        String rawId = idValue.strip();
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !rawId.contains(":")) return invalid("screen.talent_editor.layout.invalid_id");
        Optional<ResourceLocation> background = Optional.empty();
        String rawBackground = backgroundValue.strip();
        if (!rawBackground.isBlank()) {
            ResourceLocation parsedBackground = ResourceLocation.tryParse(rawBackground);
            if (parsedBackground == null || !rawBackground.contains(":")) {
                return invalid("screen.talent_editor.layout.invalid_background");
            }
            background = Optional.of(parsedBackground);
        }
        int priority = parseInt(priorityValue, -100_000, 100_000);
        int panel = parseInt(panelPercentValue, 25, 70);
        int columns = parseInt(columnsValue, 1, 8);
        int rows = parseInt(rowsValue, 1, 10);
        int slotWidth = parseInt(slotWidthValue, 24, 128);
        int slotHeight = parseInt(slotHeightValue, 22, 96);
        int iconSize = parseInt(iconSizeValue, 8, 64);
        if (priority == Integer.MIN_VALUE || panel < 0 || columns < 0 || rows < 0
                || slotWidth < 0 || slotHeight < 0 || iconSize < 0 || iconSize > slotHeight - 4) {
            return invalid("screen.talent_editor.layout.invalid_geometry");
        }
        String json = draft.toJson(
                enabled, priority, rawBackground, panel, columns, rows, slotWidth, slotHeight,
                iconSize, showGeneratedEffects, slots, pages
        );
        List<TalentBookLayoutDefinition.SlotDefinition> slotDefinitions = slots.stream()
                .map(LayoutDraft.SlotDraft::definition)
                .toList();
        List<TalentBookLayoutDefinition.PageDefinition> pageDefinitions = pages.stream()
                .map(page -> new TalentBookLayoutDefinition.PageDefinition(
                        page.id(), page.background().isBlank()
                                ? Optional.empty() : Optional.of(new ResourceLocation(page.background())),
                        page.slots().stream().map(LayoutDraft.SlotDraft::definition).toList()
                ))
                .toList();
        TalentBookLayoutDefinition definition = new TalentBookLayoutDefinition(
                id, 2, enabled, priority, background, panel, columns, rows,
                slotWidth, slotHeight, iconSize, showGeneratedEffects, slotDefinitions, pageDefinitions
        );
        return new Parsed(id, json, definition);
    }

    private Parsed invalid(String key) {
        ClientEditorState.clearValidation();
        localStatus = Component.translatable(key).getString();
        return null;
    }

    private Component enabledLabel() {
        return Component.translatable(enabled
                ? "screen.talent_editor.layout.enabled_yes" : "screen.talent_editor.layout.enabled_no");
    }

    private Component effectsLabel() {
        return Component.translatable(showGeneratedEffects
                ? "screen.talent_editor.layout.effects_yes" : "screen.talent_editor.layout.effects_no");
    }

    private Component slotLabel() {
        return Component.translatable("screen.talent_editor.layout.slots", slots.size());
    }

    private Component pageLabel() {
        return Component.translatable("screen.talent_editor.layout.pages", pages.size());
    }

    private void openSlotEditor() {
        if (minecraft != null) minecraft.setScreen(new LayoutSlotEditorScreen(this, slots, gridTemplate()));
    }

    private void openBackgroundPicker() {
        if (minecraft != null) {
            minecraft.setScreen(new ResourceReferencePickerScreen(
                    this, ResourceReferencePickerScreen.Source.TEXTURE, bootstrap,
                    id -> backgroundValue = id.toString()
            ));
        }
    }

    EditorBootstrap bootstrap() { return bootstrap; }

    void updateSlots(List<LayoutDraft.SlotDraft> value) {
        slots = List.copyOf(value);
        ClientEditorState.clearValidation();
    }

    private void openPageEditor() {
        if (minecraft != null) {
            minecraft.setScreen(new LayoutPageEditorScreen(this, pages, defaultPageSlots()));
        }
    }

    void updatePages(List<LayoutDraft.PageDraft> value) {
        pages = List.copyOf(value);
        ClientEditorState.clearValidation();
    }

    private List<LayoutDraft.SlotDraft> defaultPageSlots() {
        return slots.isEmpty() ? gridTemplate() : slots;
    }

    private List<LayoutDraft.SlotDraft> gridTemplate() {
        int columns = validOr(columnsValue, 1, 8, draft.columns());
        int rows = validOr(rowsValue, 1, 10, draft.rows());
        int width = validOr(slotWidthValue, 24, 128, draft.slotWidth());
        int height = validOr(slotHeightValue, 22, 96, draft.slotHeight());
        int icon = Math.min(validOr(iconSizeValue, 8, 64, draft.iconSize()), height - 4);
        java.util.ArrayList<LayoutDraft.SlotDraft> result = new java.util.ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                result.add(new LayoutDraft.SlotDraft(
                        "grid_" + row + "_" + column,
                        column * width, row * height, width, height, icon
                ));
            }
        }
        return List.copyOf(result);
    }

    private static int validOr(String value, int minimum, int maximum, int fallback) {
        int parsed = parseInt(value, minimum, maximum);
        return parsed == Integer.MIN_VALUE ? fallback : parsed;
    }

    private void openReferences() {
        if (!existing || minecraft == null) return;
        int count = DefinitionReferenceScreen.inboundCount(bootstrap, DefinitionKind.LAYOUT, definitionId);
        minecraft.setScreen(new DefinitionReferenceScreen(
                this, bootstrap, DefinitionKind.LAYOUT, definitionId, count
        ));
    }

    private void returnList() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new LayoutListScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() { returnList(); }

    private static int parseInt(String value, int minimum, int maximum) {
        try {
            int parsed = Integer.parseInt(value.strip());
            return parsed >= minimum && parsed <= maximum ? parsed : Integer.MIN_VALUE;
        } catch (NumberFormatException ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private record Parsed(ResourceLocation id, String json, TalentBookLayoutDefinition definition) {
    }
}
